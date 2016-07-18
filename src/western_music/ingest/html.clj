(ns western-music.ingest.html
  (:require [western-music.ingest.fetch :refer [apply-spec]]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as str]))

(def fetch-url
  (memoize (comp html/html-resource #(java.net.URL. %))))

(defn divs [html] (html/select html [:div]))

(def ^:const title-prefixes
  (cons "1." (map #(str "\n" % ".") (range 2 100))))

(defn composition-title? [^String string]    
  (some #(.startsWith string %) title-prefixes))

(def x-divs->titles
  (comp (mapcat :content)
        (filter string?)
        (filter composition-title?)))

(defn title->composer-name [title]
  (-> title
      (str/split #"- ")
      (first)
      (str/split #" ")
      (rest)
      ((partial str/join " "))))

(defn title->composition-name [title]
  (-> title
      (str/split #"- ")
      (second)
      (str/trim)))

(def ^:const arbitrary-placeholder
  "Use as a dummy keyword to wrap up some strings,
   since our generic parse is (map -> map)"
  (keyword (gensym)))

(def html-parse-spec
  {:name (comp title->composition-name
               arbitrary-placeholder)
   :composer {:name (comp title->composer-name
                          arbitrary-placeholder)}})

(defn parse-title [title]
  (-> {arbitrary-placeholder title}
      (apply-spec html-parse-spec)
      (select-keys (keys html-parse-spec))))

(defn initial-data
  "Convert the given html to a seqeunce of intial Compositions
   TODO clojure.spec all in here"
  [html]
  (into [] (comp x-divs->titles (map parse-title)) (divs html)))
