(ns western-music.handlers.map
  (:require [re-frame.core :refer [reg-event-db]]))

(reg-event-db
  :nation-ready
  (fn [data [_ nation]]
    (let [map (.-input js/map)]
      (when-not (.isActivated map nation)
        (.activateNation map nation)))
    data))
