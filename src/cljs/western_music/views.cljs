(ns western-music.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [western-music.protocols :as p])
  (:require-macros [western-music.util :refer [for']]))

(js/setTimeout
 (fn []
   (set! (.-onNationFocus (.-listeners js/map))
         #(dispatch [:focus-nation %]))
   (set! (.-onNationClick (.-listeners js/map))
         #(dispatch [:select-nation %])))
 2000)

(def ^:const icons "material-icons")

(defn composition-list
  []
  (let [tracks (subscribe [:selected-tracks])
        selected-composer (subscribe [:selected-composer])]
    (fn [parent-composer]
      (when (= (p/id @selected-composer) 
               (p/id parent-composer))
        [:ul
         (for' [track @tracks
                :let [id (p/id track)]]
           [:li {:key id}
            [:div 
             [:i
              {:on-click #(dispatch [:play-track id])
               :class icons}
              "play_arrow"]
             [:i
              {:on-click #(dispatch [:enqueue-track id])
               :class icons}
              "queue_music"]
             (p/display track)]])]))))

(defn composer-list
  []
  (let [composers (subscribe [:selected-composers])
        nation (subscribe [:selected-nation])]
    (fn []
      [:div
       [:ul (p/display @nation)
        (for' [composer @composers
               :let [id (p/id composer)]]
          [:li {:key id}
           [:div {:on-click #(dispatch [:select-composer id])} (p/display composer)]
           [composition-list composer]])]])))

(defn track-composer
  "SUPER BIG HACK separating notion of display value and internal ref id
   will go a long way towards fixing this"
  [track]
  (first (clojure.string/split track #" - ")))

(defn track-composition
  "SUPER BIG HACK separating notion of display value and internal ref id
   will go a long way towards fixing this"
  [track]
  (->> (clojure.string/split track #" - ")
       (rest)
       (clojure.string/join " - ")))

(defn track-queue
  []
  (let [queue (subscribe [:track-queue])
        current-track (subscribe [:current-track])]
    (fn []
      [:div#play-queue
       "Play Queue"
       [:i {:on-click #(dispatch [:clear-queue]) :class icons} "clear"]
       [:ul
        (for' [track @queue
               :let [id (p/id track)]]
          [:li {:key id}
           [:div 
            (when-not (= id (p/id @current-track))
              [:i {:on-click #(dispatch [:play-track id]) :class icons} "play_arrow"])
            [:i
             {:on-click #(dispatch [:dequeue-track id])
              :class icons}
             "clear"]
            (p/display track)]])]])))

(defn player-controls
  []
  (let [paused? (subscribe [:paused?])]
    (fn []
      [:div
       [:i {:on-click #(dispatch [:player-back]) :class icons} "skip_previous"]
       (if @paused?
         [:i {:on-click #(dispatch [:player-play]) :class icons} "play_arrow"]
         [:i {:on-click #(dispatch [:player-pause]) :class icons} "pause"])
       [:i {:on-click #(dispatch [:player-forward]) :class icons} "skip_next"]])))

(defn wmtm-app []
  [:div#app-body
   [:div#map-container
    [:div.mapWrapper
     [:div#map]
     [:div#text]]]
   [:div#non-map
    [:div#youtube-player]    
    [player-controls]
    [composer-list]
    [track-queue]]])
