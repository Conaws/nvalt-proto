(ns nvalt-proto.core
  (:require [loom.alg       :as galg]
            [loom.graph     :as g   ]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.walk   :refer [postwalk]])
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
"So, my intution about how this SHOULD work was quite false -- in a graph where only one directed edge caused a cycle, this function returns all the members of the cycle, going in both directions -- I can see value in detecting the single connection that causes a cycle, but don't think this solution is ideal way to address the problem"

  {:in-type 'Loom.Digraph
   :in      '{:a #{:b}
              :b #{:c :d}
              :c #{:a}
              :d #{}}
   :out     '{:c #{:b :a}
              :b #{:c :a}
              :a #{:c :b}}
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


;;; Test failing expectation 
;;; A better name for what this actually does might be, find potential cycles? It doesn't do that, I can't see any reason why this should include links that don't exist in input in the return results. 
;; It should return either the link that creates the lokop, or all the links that could be severed to end the loop. 
;; as it stands I don't think this function is actionable 

#_(= (or '{:c #{:a}}
         '{:c #{:a} :a #{:b} :b #{:c}})
     ;;; results in  {:c #{:b :a}, :b #{:c :a}, :a #{:c :b}} 
     (find-cycles (g/digraph {:a #{:b}
                              :b #{:c :d}
                              :c #{:a}
                              :d #{}})))



(declare build-link-graph read-files)

#_(find-cycles (build-link-graph (read-files "./test")))


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




(defn ends-with? [pattern string]
  (< 0 (count (re-matches (re-pattern (str pattern "$")) string))))


 (< 0 (count (re-matches (re-pattern (str ".txt" "$")) "abdc.txt")))

(ends-with? ".txt" "abdc.txt")


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

#_(first (read-files "test"))


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
         [(str/upper-case n) (get-linkset t)]))
       (into {})
       g/digraph))

;#_(= (build-link-graph (read-files "./test")) 
;#loom.graph.BasicEditableDigraph{:nodeset #{"FILE 3" "FILE2" "FILE 2" "FILE4" "FILE 4" "FILE1" "FILE 1" "FILE3"}, :adj {"FILE1" #{"FILE 3" "FILE 2"}, "FILE2" #{"FILE 3"}, "FILE3" #{"FILE 4"}, "FILE4" #{"FILE 1"}}, :in {"FILE 3" #{"FILE2" "FILE1"}, "FILE 2" #{"FILE1"}, "FILE 4" #{"FILE3"}, "FILE 1" #{"FILE4"}}}) 


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


;(defn treeify-with-content
;  "Converts a (possibly cyclic) directed graph to a tree,
;   replacing its children with their content."
;  [shortest-paths cycles content-map]
;  (->> shortest-paths))


#_(defn tests []
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
       ;print-page*
       (#(spit target  % :append true))))
 
 
(defn org-from-nvalt [target dir]
  (map (partial fileout target) (read-files dir)))
            

#_(org-from-nvalt "./nvalt-proto/test.org" "./test")


(defn mypath [] 
  (-> (java.io.File. ".") .getAbsolutePath))





;;;;;;;;;;;;;;;;;;;;;;;;;

(def thispath  "./src/clj/nvalt_proto/core.clj") 

(defn file-path->code [^String path]
  (binding [#_*read-eval* #_false]
    (read-string (str "(do " (slurp path) ")"))))

(defn find-defns [^String path]
  (let [; list, etc. format
        code (file-path->code path)
        fn-sym (atom [])]
    (postwalk
      (fn [x]
        (println x)
        (cond (list? x)
          (if (= 'defn (first x))
              (let [sym (second x)]
                (swap! fn-sym conj sym))))
        x)
      code)
    @fn-sym))

(def thiscode (file-path->code thispath))

(find-defns thiscode)

(defn atomic?
  {:todo ["Might be simpler to just do (not (coll? x))"]}
  [x]
  (or (symbol?  x)
      (string?  x)
      (number?  x)
      (keyword? x)))

(defn ->atomic-components
  {:tests '{(defn abc [a b] (+ 1 2))
            [defn abc a b + 1 2]}}
  [x]
  (let [atomic-components (atom [])]
    (postwalk
      (fn [elem]
        (when (atomic? elem)
          (swap! atomic-components conj elem)))
      x)
    @atomic-components))


#_(postwalk (fn [x] (println x)) '(+ 1 2 3))

(defn find-defns-and-vals [^String path]
  (let [; list, etc. format
        code (file-path->code path)
        fn-sym (atom {})]
    (postwalk
      (fn [x]
        (cond (list? x)
          (if (= 'defn (first x))
              (let [sym (second x)]
                (swap! fn-sym assoc sym {:body              x
                                         :atomic-components (->atomic-components x)}))))
        x)
      code)
    @fn-sym))

 #_(= (find-defns-and-vals "./src/clj/nvalt_proto/core.clj") 
  [{'mypath '(defn mypath [] 
                (-> (java.io.File. ".") .getAbsolutePath))}])


#_(let [path "./src/clj/nvalt_proto/core.clj"]
  (binding [#_*read-eval* #_false]
    (read-string (str "(do " (slurp path) ")"))))


(last (find-defns-and-vals "./src/clj/nvalt_proto/core.clj"))




;(find-references-in-map [m attr])



(def smap ; sample "directed graph"
  {:a {:children [:d]}
   :b {:children [:a]}
   :c {:children [:b :a]}
   :d {:children [:b :c :a :a]}})

(defn map-vals [f coll]
  (map (fn [[k v]] [k (f v)]) coll))

(defn map-keys [f coll]
  (map (fn [[k v]] [(f k) v]) coll))

(defn filter-vals [pred coll]
  (filter (fn [[k v]] (pred v)) coll))

(defn filter-keys [pred coll]
  (filter (fn [[k v]] (pred k)) coll))





(defn ->multigraph
  {:tests '{(->multigraph smap :children)
            {:a [:d]
             :b [:a]
             :c [:b :a]
             :d [:b :c :a :a]}}}
  [m k]
  (->> m
       (map-vals k)
       (into {})))

(defn keys-containing
  "Takes a multigraph @m and returns all keys
   whose vals contain @v."
  {:tests '{(-> smap
                (->multigraph :children)
                (keys-containing :a))
            [:b :c :d]}}
  [m v]
  (->> m
       (filter-vals (partial some #{v}))
       (mapv first)))

#_(def ^{:tests '{(inc-all [1 2 3])
                  [4 5 6]}}
  inc-all (partial map inc))

(defn invert-multigraph
  {:tests '{(invert-multigraph
              {:a [:d]      
               :b [:a]      
               :c [:b :a]   
               :d [:b :c :a]})
            {:a [:b :c :d]
             :b [:c :d]   
             :c [:d]      
             :d [:a]      }}}
  [m]
  (->> (for [k (keys m)]
         [k (keys-containing m k)])
       (into {})))

(defn merge-inversion
  "Transforms @m to a multigraph, inverts it, and
   merges the result back into @m with the @parent-attr key."
  {:test '{(merge-inversion smap :children :parents)
           {:a {:children [:d]       :parents [:b :c :d]}
            :b {:children [:a]       :parents [:c :d]   }
            :c {:children [:b :a]    :parents [:d]      }
            :d {:children [:b :c :a] :parents [:a]      }}}}
  [m child-attr parent-attr]
  (->> m
       (#(->multigraph % child-attr))
       invert-multigraph
       (map-vals #(hash-map parent-attr %))
       (merge-with conj smap)))


(defn add-parents [m] 
  (merge-inversion m :children :parents))





(add-parents smap)

(map-vals #(hash-map :parents %) (->multigraph smap :children) )
