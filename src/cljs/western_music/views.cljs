(ns western-music.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [western-music.protocols :as p]))

(def interpose' (comp doall interpose))

(def ^:const icons "material-icons")

(def ^:const divider [:div.list-divider])

(def with-dividers (partial interpose' divider))

(defn icon
  ([on-click type] (icon {} on-click type))
  ([options on-click type]
   (let [class (if (:class options)
                 (str icons " " (:class options))
                 icons)]
     [:i (assoc options
                :on-click on-click
                :class class)
      type])))

(defn composition-list
  []
  (let [tracks (subscribe [:selected-tracks])
        queue (subscribe [:track-queue])]
    (fn []
      [:div#composer-tracks
       (with-dividers
         (for [track @tracks
                :let [id (p/id track)]]
           [:div.track-list-item {:key id}
            (icon #(dispatch [:play-track id]) "play_arrow")
            (icon #(dispatch [:enqueue-track id]) "queue_music")
            [:div.list-item-text
             {:on-click (if (empty? @queue)
                          #(dispatch [:play-track id])
                          #(dispatch [:enqueue-track id]))}
             (p/display track)]]))])))

(defn composer-list
  []
  (let [composers (subscribe [:selected-composers])
        selected-composer (subscribe [:selected-composer])
        nation (subscribe [:selected-nation])]
    (fn []
      [:div#selection-list
       (when-not (nil? @nation)
         [:h2 (p/display @nation)])
       (with-dividers
         (for [composer @composers
               :let [id (p/id composer)]]
           [:div {:key id :on-click #(dispatch [:select-composer id])}
            (p/display composer)
            (when (= id (p/id @selected-composer))
              [composition-list])]))])))

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
       (with-dividers
         (for [track @queue
                :let [id (p/id track)]]
           [:div.track-list-item {:key id}
            (when-not (= id (p/id @current-track))
              (icon #(dispatch [:play-track id]) "play_arrow"))
            (icon #(dispatch [:dequeue-track id]) "clear")
            (p/display track)]))])))

(defn player-controls
  []
  (let [paused? (subscribe [:paused?])]
    (fn []
      [:div
       (icon #(dispatch [:player-back]) "skip_previous")
       (if @paused?
         (icon #(dispatch [:player-play]) "play_arrow")
         (icon #(dispatch [:player-pause]) "pause"))
       (icon #(dispatch [:player-forward]) "skip_next")])))

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
