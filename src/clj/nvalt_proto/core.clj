(ns nvalt-proto.core
  (:require [loom.alg       :as galg]
            [clojure.pprint
              :refer [pprint]]
            [clojure.string :as str])
  (:import java.io.File))

(defn dropl
  "Drop n elements from left end of a sequence. Eager"
  [n s]
  (cond (string? s)
        (subs s n (count s))))

(defn dropr
  "Drop n elements from right end of a sequence. Eager"
  [n s]
  (cond (string? s)
        (subs s 0 (- (count s) n))))

(defn read-files
  "Slurps the .txt files in a given directory.
   Outputs a map of <file-name> -> <file-text>"
  [path]
  (->> path
       (File.)
       file-seq
       (filter #(-> % str (.endsWith #_str/ends-with? ".txt")))
       (map    (juxt #(->> % .getName (dropr 4)) slurp))
       (into {})))

(defn extract-links-from-files
  [files]
  (->> files
       (map (fn [[file-name file-text]]
         [file-name (->> file-text
                         (re-seq #"\[\[.*\]\]")
                         (map #(->> %
                                    (dropl 2)
                                    (dropr 2)))
                         (into #{}))]))
       (into {})))

(defn tests []
  (let [files (-> "./nvalt-proto"
                  read-files
                  (doto pprint))
        graph (-> files
                  extract-links-from-files
                  (doto pprint))]
  ))

(tests)