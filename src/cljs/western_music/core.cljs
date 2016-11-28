(ns western-music.core
  (:require [western-music.views :refer [wmtm-app]]
            [western-music.handlers]
            [western-music.subscriptions]
            [western-music.edn :refer [register-custom-readers!]]
            [re-frame.core :refer [dispatch reg-event-fx]]
            [reagent.core :as reagent]
            [western-music.lib.ui :as ui]
            [day8.re-frame.http-fx]
            [ajax.edn :as edn]
            [western-music.handlers.youtube]
            [western-music.handlers.map]))

(def current-seconds #(.getTime (js/Date.)))

(defn cache-bust [url]
  (->> (current-seconds)
       (str "?&_=")
       (str url)))

(reg-event-fx
  :initialize-data
  ui/verify-all-data
  (fn [_ _]
    {:db ui/initial-data
     :http-xhrio {:method :get
                  :uri (cache-bust "edn/compositions.edn")
                  :body {}
                  :response-format (edn/edn-response-format)
                  :on-success [:compositions-from-server]}}))

(defn set-map-listeners! []
  (set! (.-onNationFocus (.-listeners js/map))
        #(dispatch [:focus-nation %]))
  (set! (.-onNationClick (.-listeners js/map))
        #(dispatch [:select-nation %]))
  (set! (.-onNationBlur (.-listeners js/map)) identity))

(defn ^:export main
  []
  (register-custom-readers!)
  (reagent/render [wmtm-app] (.getElementById js/document "app")
                  (fn []
                    (set-map-listeners!)
                    (.initialize js/map)
                    (dispatch [:initialize-data])
                    (dispatch [:initialize-player]))))
