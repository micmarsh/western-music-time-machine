(ns western-music.util
  (:require [clojure.walk :as walk]))

(defn merge-recursive [& maps]
  (apply merge-with (fn [val1 val2]
                      (if (every? map? [val1 val2])
                        (merge-recursive val1 val2)
                        val2))
         maps))

(defn conditional-prewalk [pred? f]
  (fn [data]
    (walk/prewalk
     (fn [v] (if (pred? v) (f v) v))
     data)))

(comment
  (merge-with merge {:foo {:bar {:baz "yo"}}} {:foo {:bar {:hello "world"}}})

  (merge-recursive {:foo {:bar {:baz "yo"}}} {:foo {:bar {:hello "world"}}})
  
  )
