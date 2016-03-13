(ns nvalt-proto.core
  (:require [loom.alg       :as galg]
            [loom.graph     :as g   ]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str])
  (:import java.io.File))

; ===== COLLECTIONS FUNCTIONS ===== ;

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

; ===== GRAPH FUNCTIONS ===== ;

(defn shortest-paths
  "Returns all shortest paths from root node to all other nodes in the graph."
  [g start]
  (->> g :nodeset
       (map (fn [node] [node (galg/shortest-path g start node)]))
       (into {})))

(defn eventual-connections
  "All pairs of nodes which eventually connect.
   Calculated via Johnson's algorithm."
  [g]
  (->> g
       galg/johnson
       (map    (fn [[from tos]] [from (->> tos vals (apply merge) keys (into #{}))]))
       (remove (fn [[from tos]] (empty? tos)))
       (into {})))

(defn cycles
  "Returns all cycles in a directed graph @g."
  [g]
  (let [eventual (eventual-connections g)]
    (->> eventual
         (map (fn [[from tos]]
                [from (->> tos
                           (filter (fn [to] (or (-> eventual (get to  ) (get from  ) nil? not)
                                                (-> eventual (get to  ) (get to    ) nil? not)
                                                (-> eventual (get from) (get from  ) nil? not))))
                           (into #{}))]))
         (remove (fn [[from tos]] (empty? tos)))
         (into {}))))

(defn depths-from-node
  [shortest-paths node]
  (->> shortest-paths
       (remove (fn [[to path]] (nil? path)))
       (map    (fn [[to path]] [to (-> path count dec)]))
       (into {})
       (#(assoc % node 0))))

; ===== APP-SPECIFIC FUNCTIONS ===== ;

(defn read-files
  "Slurps the .txt files in a given directory.
   Outputs a map of <file-name> -> <file-text>"
  [path]
  (->> path
       (File.)
       file-seq
       (filter #(-> % str (.endsWith #_str/ends-with? ".txt")))
       (map    (juxt #(->> % .getName (dropr 4) str/upper-case) slurp))
       (into {})))

(defn extract-links-from-file
  "Extracts [[]] links from text in file.
   Ensures that all links are capitalized."
  [file-text]
  (->> file-text
       (re-seq #"\[\[.+?\]\]")
       (map #(->> %
                  (dropl 2)
                  (dropr 2)))
       (map str/upper-case)
       (into #{})))

(defn build-graph-from-file-vecs
  "Extracts [[]] links from text in files.
   Builds graph accordingly.
   Ensures that all links are capitalized."
  {:tests '{{"file-name"  "file text is here [[link]] [[other link]]"
             "other link" "more file text 1"
             "link"       "more file text 2"}
            {"FILE-NAME"  #{"LINK" "OTHER LINK"}
             "OTHER LINK" #{}
             "LINK"       #{}}}}
  [files]
  (->> files
       (map (fn [[file-name file-text]]
         [(str/upper-case file-name) (extract-links-from-file file-text)]))
       (into {})))

(defn treeify-with-content
  "Converts a (possibly cyclic) directed graph to a tree,
   replacing its children with their content."
  [g content-map]
  )

(defn tests []
  (let [files (-> "./nvalt-proto"
                  read-files
                  (doto pprint))
        graph (-> files
                  build-graph-from-file-vecs
                  (doto pprint)
                  g/digraph)
        root-node "FEATURES"
        shortest-paths-from-root-node
          (->> (shortest-paths graph root-node)
               (remove (fn [[to path]] (not (nil? path))))
               (into {}))
        _ (do (println "===== SHORTEST PATHS FROM ROOT NODE =====")
              (pprint shortest-paths-from-root-node)
              (println "=========="))
        depths-from-root-node
          (depths-from-node graph root-node)
        _ (pprint depths-from-root-node)
        cycles-from-root
          (->> graph
               cycles
               (filter (fn [[from tos]] (contains? shortest-paths-from-root-node from)))
               (into {}))
        _ (pprint cycles-from-root)
        ;replaced
          ;(treeify-with-content shortest-paths-from-root-node files)
          ]
  graph))

(def graph (tests))

;(def test-cyclic-graph {:a #{:b :c} :b #{:a} :c #{}})


 