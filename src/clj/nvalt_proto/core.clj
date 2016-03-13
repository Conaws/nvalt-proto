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
  "Returns all shortest paths from starting node @start to all other nodes in the graph.

   Each returned key, k, is a node which is in the graph.
   Each returned val, v, is a seq, the shortest path which @start takes to k.

   A nil value with a node key means that the node is unreachable from the starting node @start."
  {:in-types '{g     Loom.Digraph
               start Any}
   :out-type 'Map
   :in      '(g/digraph
               {:a #{:b :c}
                :b #{}
                :c #{:a}})
   :out     '{:c (:a :c)
              :b (:a :b)
              :a nil}}
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

(defn find-cycles
  "Returns all cycles in a directed graph @g.

   EXAMPLE NOTES:
     a -> b
     b -> a
     Therefore there is a cycle from a <-> b."
  {:in-type 'Loom.Digraph
   :in      '(g/digraph
               {:a #{:b :c}
                :b #{}
                :c #{:a}})
   :out     '{:a #{:c}
              :c #{:a}}
   :todo    ["Perhaps it would be more intuitive to output the cycles
              in the format #{[:a :c] [:c :a]} — cycle pairs,
              where :a -> :c creates a cycle and :c -> :a creates a cycle."]}
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
  {:in-type 'String
   :in      "my-path/file.txt"}
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
  {:in-type 'String
   :in      "any text [[link]] more text"}
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
  [shortest-paths cycles content-map]
  (->> shortest-paths))

(defn tests []
  (let [files (read-files "./nvalt-proto")
        graph (-> files
                  build-graph-from-file-vecs
                  g/digraph)
        root-node "FEATURES"
        shortest-paths-from-root-node
          (->> (shortest-paths graph root-node)
               (remove (fn [[to path]] (nil? path)))
               (into {}))
        depths-from-root-node
          (depths-from-node graph root-node)
        cycles-from-root
          (->> graph
               find-cycles
               (filter (fn [[from tos]] (contains? shortest-paths-from-root-node from)))
               (into {}))
        replaced
          (treeify-with-content shortest-paths-from-root-node
            cycles-from-root files)
        _ (pprint replaced)]
  graph))





(def graph (tests))

;(def test-cyclic-graph {:a #{:b :c} :b #{:a} :c #{}})


 