(ns western-music.ingest.maps
  (:require [western-music.util :as util]))

(defprotocol Lookup
  (lookup [this data]))

(extend-protocol Lookup
  clojure.lang.Fn
  (lookup [this arg] (this arg))
  clojure.lang.Var
  (lookup [this arg] (lookup @this arg))
  clojure.lang.Symbol
  (lookup [this arg]
    (if-let [v (resolve this)]
      (lookup v arg)
      (throw (ex-info (str "No value found for " this)
                      {:symbol this})))))

(defn apply-lookups
  [map]
  (let [walk (util/conditional-prewalk (partial satisfies? Lookup)
                                  #(lookup % map))]
   (walk map)))

(def resolve-results
  (util/conditional-prewalk (partial instance? clojure.lang.IDeref) deref))

(defn apply-spec [data spec]
  (-> (merge-recursive data spec)
      (apply-lookups)
      (resolve-results)))

(comment
  (defn get-hello [data]
    (str "hello, " (:name data)))
  
  (apply-spec {:name "Ludwig"} {:greeting {:greet-with get-hello
                                           :same-greeting #'get-hello
                                           :var-gretting `get-hello}
                                :place (constantly (future "async place"))})

  
  )
