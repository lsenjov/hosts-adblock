(ns hosts-adblock.core
  (:gen-class))

(def blocklists
  [;Top sources from https://github.com/AdAway/AdAway/wiki/HostsSources
   "https://adaway.org/hosts.txt"
   "https://hosts-file.net/ad_servers.txt"
   "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext"

   "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews-gambling-porn-social/hosts"
   ])

(def hosts-file "/etc/hosts")
(def autofill-line "\n# AUTOFILL")

(defn generate-local-ip-addresses
  "Generates a lazy list of numbers in the 0.x.x.x range. Repeats if required.
  0.x.x.x range is preferred. See https://github.com/StevenBlack/hosts#we-recommend-using-0000-instead-of-127001"
  []
  (for [first (range 255) second (range 255) third (range 255)]
    (format "0.%d.%d.%d" first second third)))

(defn get-blocks
  "Return a list of hosts to block"
  []
  (->> blocklists
       (map slurp)
       ; Split and merge by newlines
       (mapcat #(clojure.string/split % #"\n"))
       ; Remove empty lines
       (remove #(zero? (count %)))
       ; Remove comments
       (remove #(= \# (first %)))
       (map #(clojure.string/split % #"\s+"))
       ; We only care about hosts
       (map second)
       (map clojure.string/trim)
       ; Remove loopbacks/defaults
       (remove #{"localhost" "localhost.localdomain" "local" "broadcasthost" "ip6-localhost" "loopback" "ip6-loopback" "ip6-localnet" "ip6-mcastprefix" "ip6-allnodes" "ip6-allrouters" "ip6-allhosts" "0.0.0.0" "#"})
       (distinct)
       (sort)
       ; Give each host a unique IP address, so if you see something pinging out to a loopback address you know which host it is
       (map (fn [ip host] (format "%s\t%s" ip host)) (generate-local-ip-addresses))
       ))

(defn get-hosts
  "Gets current hosts, before the (maybe existing) cutoff line"
  []
  (-> hosts-file
      (slurp)
      ; Split on whatever we've decided is our autofill token
      (clojure.string/split (re-pattern autofill-line))
      ; Ignore everything after (and including) the token
      (first)))

(defn apply-blocks
  "Actually get the hosts, and put it into the hosts file"
  []
  (let [hosts (interpose \newline (get-blocks))]
    ; It's nice to know how many hosts we're blocking
    (println "Applying" (count hosts) "host records")
    (spit hosts-file (apply str (get-hosts) autofill-line \newline hosts))))

(defn -main
  "Go get the blocklists, put it into the /etc/hosts file"
  [& args]
  (println "Begin")
  (apply-blocks)
  (println "Done")
  )
