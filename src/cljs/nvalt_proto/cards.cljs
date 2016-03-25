(ns nvalt-proto.cards
  (:require [cljs.core.async :as async :refer [<! >!]]
            [cljs-http.client :as http])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(println "Here")

(defonce wiki (atom {}))

(add-watch wiki :watch
  (fn [a k oldv newv]
    (println newv)))
(js/console.clear)

(defn get-wiki [db name]
  (go (swap! wiki assoc name
      (<! (http/jsonp  "https://en.wikipedia.org/w/api.php"
            {:callback-name "callback"
             :query-params {:action "query"
              :prop   "categories"
              :titles name
              :format "json"
              :callback "json"}})))))

#_(->> (mapv (partial get-wiki wiki)
          ["Nikola Tesla" "Elon Musk" "Benjamin Franklin" "Thomas Jefferson"]))

(defn map-vals [f coll]
  (map (fn [[k v]] [k (f v)]) coll))

(def ffilter (comp first (partial filter)))

(defn wiki-results->category-titles [results]
  (->> results :body :query :pages first val :categories (map :title)))

(def ^{:doc "The last index in an indexed collection"}
  lasti (comp dec count))

(defn first-matching-category [category-type categories]
  (let [to-find (re-pattern (str "Category:(\\d+) " category-type))]
    (second (first (filter second (map (partial re-find to-find) categories))))))


(def died (partial first-matching-category "deaths"))
(def born (partial first-matching-category "births"))

(->> @wiki
     (map-vals wiki-results->category-titles)
     (map-vals (juxt born died)))







(defn extract-info-from-category [category-type category]
  (when category
    (.substring category
      (count "Category:")
      (- (lasti category) (count category-type)))))

(defn str->int [x]
  (when (string? x)
    (js/parseInt x)))

(defn extract-date [category-type categories]
  (->> categories
       (first-matching-category category-type)
       (extract-info-from-category category-type)
       str->int))

(defn extract-dates [labels category-types categories]
  (zipmap labels
    ((apply juxt (map (fn [category-type]
                        (partial extract-date category-type))
                   category-types))
     categories)))

(defn db->extract-dates-and-names [db]
  (->> @db
       (map-vals (fn [v]
                   (let [categories (wiki-results->category-titles v)]
                     (extract-dates [:birth :death] ["births" "deaths"] categories))))
       (into {})))

(println (db->extract-dates-and-names wiki))

; So you do the following: (-> response :body js/JSON.parse json->map :query :pages first )
