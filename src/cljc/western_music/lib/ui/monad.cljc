(ns western-music.lib.ui.monad
  "Represent re-frame \"effects\" as a monad, to enable composition of 
   various (database -> effects) functions")

(defn return [db] {:db db})

(defn merge-events [fx1 fx2]
  (let [events (concat (keep :dispatch [fx1 fx2]) (mapcat :dispatch-n [fx1 fx2]))]
    (cond (empty? events) {}
          (= 1 (count events)) {:dispatch (first events)}
          :else {:dispatch-n events})))

;; TODO could TDD up proper dispatch-later merging

(def ^:const events-keys [:dispatch :dispatch-n])

(defn fx= [& fxs]
  (let [all-events (for [fx fxs]
                     (if-let [event (:dispatch fx)]
                       (cons event (:dispatch-n fx))
                       (:dispatch-n fx)))]
    (doseq [events all-events]
      (assert (= (count events) (count (set events)))
              "Can't properly compare with \"duplicate\" events present"))
    (and (apply = (map set all-events))
         (apply = (map #(apply dissoc % events-keys) fxs)))))

(defn bind [fx db-f & args]
    (let [new-fx (apply db-f (:db fx) args)]
      (-> fx
          (merge-events new-fx)
          (merge (apply dissoc new-fx events-keys)))))

(defn fmap [fx db-f & args] (apply update fx :db db-f args))
