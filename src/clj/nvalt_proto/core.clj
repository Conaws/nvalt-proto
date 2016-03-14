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
"Todo, test if intuition of example is correct"
  {:in-type 'Loom.Digraph
   :in      '{:a #{:b}
              :b #{:c :d}
              :c #{:a}
              :d #{}}
   :out     '{:c #{:a}}}
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


;;(comment 
;;"Removing these"
;;
;;(defn extract-links-from-file
;;  "Extracts [[]] links from text in file.
;;   Ensures that all links are capitalized."
;;  {:in-type 'String
;;   :in      "any text [[link]] more text"}
;;  [file-text]
;;  (->> file-text
;;       (re-seq #"\[\[.+?\]\]")
;;       (map #(->> %
;;                  (dropl 2)
;;                  (dropr 2)))
;;       (map str/upper-case)
;;       (into #{})))
;;
;;(defn build-graph-from-file-vecs
;;  "Extracts [[]] links from text in files.
;;   Builds graph accordingly.
;;   Ensures that all links are capitalized."
;;  {:tests '{{"file-name"  "file text is here [[link]] [[other link]]"
;;             "other link" "more file text 1"
;;             "link"       "more file text 2"}
;;            {"FILE-NAME"  #{"LINK" "OTHER LINK"}
;;             "OTHER LINK" #{}
;;             "LINK"       #{}}}}
;;  [files]
;;  (->> files
;;       (map (fn [[file-name file-text]]
;;         [(str/upper-case file-name) (extract-links-from-file file-text)]))
;;       (into {}))))
;;


(defn treeify-with-content
  "Converts a (possibly cyclic) directed graph to a tree,
   replacing its children with their content."
  [shortest-paths cycles content-map]
  (->> shortest-paths))


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
               (filter (fn [[from tos]] (contains? shortest-paths-from-root-node from)))
               (into {}))
        replaced
          (treeify-with-content shortest-paths-from-root-node
            cycles-from-root files)
        _ (pprint replaced)]
  graph))



(defn logger [x]
  (do 
    (pprint x)) 
  x)




(defn header-stars [depth]
  (apply str (repeat depth "*")))

#_(= "*****"
 (header-stars 5))




(defn print-page [{:keys [name text depth]}]
  (str (header-stars depth) " " name " \n\t" text))


(defn fileout [target file]
  (->> file 
       (zipmap [:name :text])
       (merge {:depth 1})
       print-page*
       (#(spit target  % :append true))))
 
 
(defn org-from-nvalt [target dir]
  (map (partial fileout target) (read-files dir)))
            

#_(org-from-nvalt "./nvalt-proto/test.org" "./test")


(defn mypath []
  (-> (java.io.File. ".") .getAbsolutePath))
