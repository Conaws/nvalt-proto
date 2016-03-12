(ns nvalt-proto.core
  (:require [loom.alg       :as galg]
            [clojure.pprint
              :refer [pprint]]
            [clojure.string :as str])
  (:import java.io.File))

(defn dropr
  "Drop n elements from right end of a sequence"
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
       (map [[file-name file-text]]
         [file-name (->> file-text
                         (re-seq #"\[\[.*\]\]")
                         (into #{}))])
       (into {})))

(defn tests []
  (pprint (read-files               "./nvalt-proto"))
  (pprint (extract-links-from-files "./nvalt-proto")))

(tests)