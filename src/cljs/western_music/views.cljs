(ns western-music.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [western-music.protocols :as p]))

(def ^:const icons "material-icons")

(defn ->divider []
  [:div.list-divider {:key (gensym)}])

(defn with-dividers [s]
  (doall (drop 1 (interleave (repeatedly ->divider) s))))

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

(defn nation-header
  []
  (let [nation (subscribe [:selected-nation])
        composers (subscribe [:selected-composers])
        queue (subscribe [:track-queue])]
    (fn []
      (if (nil? @nation)
        [:h3 "(select a nation from the map)"]
        [:div#nation-header
         [:h2 (p/display @nation)]
         (when-not (empty? @composers)
           (if (empty? @queue)
             (icon #(dispatch [:play-nation (p/id @nation)]) "play_arrow")
             (icon #(dispatch [:enqueue-nation (p/id @nation)]) "queue_music")))]))))

(defn composer-list
  []
  (let [composers (subscribe [:selected-composers])
        selected-composer (subscribe [:selected-composer])
        queue (subscribe [:track-queue])]
    (fn []
      [:div
       (with-dividers
         (for [composer @composers
               :let [id (p/id composer)]]
           [:div {:key id}
            [:div.composer-header
             (icon {:class "smaller-icons"} #(dispatch [:select-composer id])
                   (if (= id (p/id @selected-composer))
                     "expand_more"
                     "chevron_right"))
             [:div {:on-click #(dispatch [:select-composer id])}
              (p/display composer)]
             (if (empty? @queue)
               (icon #(dispatch [:play-composer id]) "play_arrow")
               (icon #(dispatch [:enqueue-composer id]) "queue_music"))]
            (when (= id (p/id @selected-composer))
              [composition-list])]))])))

(defn selection-list
  []
  [:div#selection-list [nation-header] [composer-list]])

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
  (let [paused? (subscribe [:paused?])
        shuffle? (subscribe [:shuffle-on?])]
    (fn []
      [:div#player-controls
       (icon #(dispatch [:player-back]) "skip_previous")
       (if @paused?
         (icon #(dispatch [:player-play]) "play_arrow")
         (icon #(dispatch [:player-pause]) "pause"))
       (icon #(dispatch [:player-forward]) "skip_next")
       (if @shuffle?
         [:button {:on-click #(dispatch [:shuffle-off])} "shuffle off"]
         (icon #(dispatch [:shuffle-on]) "shuffle"))])))

(defn track-tabs []
  (let [q (subscribe [:track-queue])]
    (fn []
      [:div#tabs
       [:input#selection-tab {:type "radio" :name "grp"}]
       [:label {:for "selection-tab"} "Selection"]
       [:div.tab-content [selection-list]]
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
