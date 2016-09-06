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

(defn string= [& strings]
 (->> strings
      (map #?(:cljs #(str (js/String. %)) :clj identity))
      (apply =)))

(def ^:dynamic *global-cache* nil)

(defn debug-memoize
  "Allows optional debugging override of cache"
  [f]
  (let [cache (atom {})]
    (fn [& args]
      (let [mem (or *global-cache* cache)]
        (if-let [e (find @mem args)]
          (val e)
          (let [ret (apply f args)]
            (swap! mem assoc args ret)
            ret))))))

(defmacro defcached
 "Define a memoized + syncronized, single argument function"
 [name doc args & body]
 `(let [memoized# (debug-memoize (fn [~(first args)] ~@body))]
    (defn ~name [arg#] (locking arg# (memoized# arg#)))))

(defmacro for' [& parts]
  `(doall (for ~@parts)))

(defprotocol RandomGen
  (random [obj]))

(extend-protocol RandomGen
  #?(:clj clojure.lang.ISeq
     :cljs cljs.core.PersistentVector)
  (random [s] (rand-nth s)))

(defn rand-mem
  ([already-chosen? gen]
   (rand-mem already-chosen? gen 200))
  ([already-chosen? gen tries]
   (let [item (random gen)]
     (cond
       (= tries 0) ::generator-exhausted
       (already-chosen? item) (recur already-chosen? gen (dec tries))
       :else item))))

(comment
  (merge-with merge {:foo {:bar {:baz "yo"}}} {:foo {:bar {:hello "world"}}})
  (merge-recursive {:foo {:bar {:baz "yo"}}} {:foo {:bar {:hello "world"}}})

  )
