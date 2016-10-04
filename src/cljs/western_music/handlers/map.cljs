(ns western-music.handlers.map
  (:require [re-frame.core :refer [reg-event-fx reg-fx]]))

(reg-fx
 :activate-nation
 (fn [[map* nation]]
   (let [map (.-input map*)]
     (when-not (.isActivated map nation)
       (.activateNation map nation)))))

(reg-event-fx
 :nation-ready
 (fn [_ [_ nation]]
   (if (nil? js/map)
     {:dispatch-later [{:ms 1000 :dispatch [:nation-ready nation]}]}
     {:activate-nation [js/map nation]})))
