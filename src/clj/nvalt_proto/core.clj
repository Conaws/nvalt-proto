(ns nvalt-proto.core
  (:require [loom.alg       :as galg]
            [clojure.pprint
              :refer [pprint]]
            [clojure.string :as str ]))

(defn read-files [path]
  (->> path
       file-seq
       (filter #(-> % str (str/ends-with? ".txt")))
       (map    (juxt str slurp))
       ))

(defn tests []
  (pprint (read-files "/Users/alexandergunnarson/Development/Source Code Projects/junto-labs/nvalt-proto/nvalt-proto")))

(tests)