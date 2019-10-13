(ns hosts-adblock.core
  (:gen-class))

(def blocklists
  "Top sources from https://github.com/AdAway/AdAway/wiki/HostsSources"
  ["https://adaway.org/hosts.txt"
   "https://hosts-file.net/ad_servers.txt"
   "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext"
   ])

(def hosts-file "/etc/hosts")
(def autofill-line "\n# AUTOFILL")

(defn generate-local-ip-addresses
  "Generates a lazy list of numbers in the 127.x.x.x range. Repeats if required"
  []
  (for [first (range 255) second (range 255) third (range 255)]
    (format "127.%d.%d.%d" first second third)))

(defn get-blocks
  "Return a list of hosts to block"
  []
  (->> blocklists
       (map slurp)
       (mapcat #(clojure.string/split % #"\n"))
       (remove #(zero? (count %)))
       (remove #(= \# (first %))) ; Remove comments
       (map #(clojure.string/split % #"\s+"))
       (map second)
       (distinct)
       (sort)
       (map (fn [ip host] (format "%s\t%s" ip host)) (generate-local-ip-addresses))
       ))

(defn get-hosts
  "Gets current hosts, before the (maybe existing) cutoff line"
  []
  (-> hosts-file
      (slurp)
      (clojure.string/split (re-pattern autofill-line))
      (first)))

(defn apply-blocks
  "Actually get the hosts, and put it into the hosts file"
  []
  (let [hosts (interpose \newline (get-blocks))]
    (println "Applying" (count hosts) "host records")
    (spit hosts-file (apply str (get-hosts) autofill-line \newline hosts))))

(defn -main
  "Go get the blocklists, put it into the /etc/hosts file"
  [& args]
  (println "Begin")
  (apply-blocks)
  (println "Done")
  )
