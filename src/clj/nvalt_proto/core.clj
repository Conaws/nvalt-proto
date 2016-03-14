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
  "Returns all shortest paths from node @start to all other nodes in the graph."
  {:in-type '{g     Loom.Digraph
              start Any}
   :in      '{}
   :out     '{}}
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
   :in      '{:a #{:b}
              :b #{:c :d}
              :c #{:a}
              :d #{}}
   :out     '{:a #{:b}
              :b #{:a}}}
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


(defn is-text-file? 
  {:test (is-text-file? "abce.txt")}
  [file-name] 
    (.endsWith #_str/ends-with? file-name ".txt"))


(defn file-name 
  {:test (file-name (first (file-seq (File. "nvalt-proto"))))}
  [file-object]
  (->> file-object
       .getName 
       (dropr 4)))



(def file-to-vec 
  (partial (juxt 
            file-name
;            uppercasing isn't needed because .getName returns an uppcased
;            (comp str/upper-case file-name) 
            slurp)))

#_(file-to-vec (last (file-seq (File. "./"))))



(defn read-files
  [path]
  (->> path
       (File.)
       file-seq
       (filter #(is-text-file? (str %)))
       (map file-to-vec)))

#_(last (read-files "nvalt-proto"))



(def remove-brackets
  #(->> %
    (dropl 2)
    (dropr 2)))

#_(=  (remove-brackets "[[abcd]]")
      "abcd")


(def link-seq
 (partial re-seq #"\[\[.+?\]\]"))

#_(= (link-seq "[[li]] is a [[link]] is a [[link]]")
   '("[[li]]" "[[link]]" "[[link]]"))

(defn get-linkset [text]
  (->> text
       link-seq
       (map (comp remove-brackets str/upper-case))
       set))
 
#_(= (get-linkset "[[li]] is a [[link]] is a [[link]]")
 #{"LI" "LINK"})


(defn build-link-graph
  [files]
  (->> files
       (map (fn [[n t]]
         [(str/upper-case n) (get-linkset t))]))
       (into {})
       g/digraph))

(= (build-link-graph (read-files "./test")) 
#loom.graph.BasicEditableDigraph{:nodeset #{"FILE 3" "FILE2" "FILE 2" "FILE4" "FILE 4" "FILE1" "FILE 1" "FILE3"}, :adj {"FILE1" #{"FILE 3" "FILE 2"}, "FILE2" #{"FILE 3"}, "FILE3" #{"FILE 4"}, "FILE4" #{"FILE 1"}}, :in {"FILE 3" #{"FILE2" "FILE1"}, "FILE 2" #{"FILE1"}, "FILE 4" #{"FILE3"}, "FILE 1" #{"FILE4"}}}) 


(defn tests []
  (let [files (read-files "./nvalt-proto")
        graph (build-link-graph files)
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
               ;; my point here is that 
               (filter (fn [[from tos]] (contains? shortest-paths-from-root-node from)))
               (into {}))
        replaced
          (treeify-with-content shortest-paths-from-root-node
            cycles-from-root files)
        _ (pprint replaced)]
  graph))



;; Outputing a single file
;; Take a file name, depth, and the text
;; print a string where there are stars equal to the depth





(defn header-stars [depth]
  (apply str (repeat depth "*")))

(= "*****"
 (header-stars 5))

(header-stars 1)

;;output a single file

(defn spit-pretty [target val] 
 (spit target (with-out-str (pprint val))))

(defn output [val]
  (spit-pretty "./nvalt-proto/1.org" val))

;; Note -- using partial application here printed out a lazy sequence, so used defn 

(defn logger [x]
  (do 
    (pprint x)) 
  x)



(defn fileout* [file]
  (->> file 
       (zipmap [:name :text])
       (merge {:depth 1})
       print-page
       (#(spit "./nvalt-proto/2.org" % :append true))))
 
 
(map fileout* (read-files "./nvalt-proto"))
            







(def graph (tests))


(defn treeify-with-content
  "Converts a (possibly cyclic) directed graph to a tree,
   replacing its children with their content."
  [shortest-paths cycles content-map]
  (->> shortest-paths))


;(def test-cyclic-graph {:a #{:b :c} :b #{:a} :c #{}})


 
