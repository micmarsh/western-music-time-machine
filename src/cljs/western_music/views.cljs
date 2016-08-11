(ns western-music.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [western-music.protocols :as p])
  (:require-macros [western-music.util :refer [for']]))

(def ^:const icons "material-icons")

(defn composition-list
  []
  (let [tracks (subscribe [:selected-tracks])
        queue (subscribe [:track-queue])]
    (fn []
      [:div#composer-tracks
       (for' [track @tracks
              :let [id (p/id track)]]
         [:div {:key id
                :on-click (if (empty? @queue)
                            #(dispatch [:play-track id])
                            #(dispatch [:enqueue-track id]))}
          [:i
           {:on-click #(dispatch [:play-track id])
            :class icons}
           "play_arrow"]
          [:i
           {:on-click #(dispatch [:enqueue-track id])
            :class icons}
           "queue_music"]
          (p/display track)])])))

(defn composer-list
  []
  (let [composers (subscribe [:selected-composers])
        selected-composer (subscribe [:selected-composer])
        nation (subscribe [:selected-nation])]
    (fn []
      [:div#selection-list
       [:h2 (p/display @nation)]
       (for' [composer @composers
              :let [id (p/id composer)]]
         [:div {:key id :on-click #(dispatch [:select-composer id])}
          (p/display composer)
          (when (= id (p/id @selected-composer))
            [composition-list])])])))

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
;       "Play Queue"
;       [:i {:on-click #(dispatch [:clear-queue]) :class icons} "clear"]
       (for' [track @queue
              :let [id (p/id track)]]
         [:div {:key id}
          (when-not (= id (p/id @current-track))
            [:i {:on-click #(dispatch [:play-track id]) :class icons} "play_arrow"])
          [:i
           {:on-click #(dispatch [:dequeue-track id])
            :class icons}
           "clear"]
          (p/display track)])])))

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

(defn track-tabs []
  (let [q (subscribe [:track-queue])]
    (fn []
      [:div#tabs
       [:input#selection-tab {:type "radio" :name "grp"}]
       [:label {:for "selection-tab"} "Selection"]
       [:div.tab-content [composer-list]]
       [:input#queue-tab {:type "radio" :name "grp"}]
       [:label {:for "queue-tab"}
        (str "Play Queue" (when-not (empty? @q) (str " (" (count @q) ")")))]
       [:div.tab-content [track-queue]]])))

(defn wmtm-app []
  [:div#app-body
   [:div#map-container
    [:div.mapWrapper
     [:div#map]
     [:div#text]]]
   [:div#non-map
    [:div#youtube-player]    
    [player-controls]
    [track-tabs]]])
