(ns western-music.lib.ui.monad
  "Represent re-frame \"effects\" as a monad, to enable composition of 
   various (database -> effects) functions")

(defn return [db] {:db db})

(defn merge-events [fx1 fx2]
  (let [events (concat (keep :dispatch [fx1 fx2]) (mapcat :dispatch-n [fx1 fx2]))]
    (if-not (empty? events)
      {:dispatch-n events}
      {})))

(defn bind [fx db-f & args]
    (let [new-fx (apply db-f (:db fx) args)]
      (-> fx
          (merge-events new-fx)
          (merge (dissoc new-fx :dispatch :dispatch-n)))))

(defn fmap [fx db-f & args] (apply update fx :db db-f args))
