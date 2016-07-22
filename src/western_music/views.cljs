(ns western-music.views
  (:require [re-frame.core :refer [subscribe dispatch]]))


(defn dummy-nation-list
  []
  (fn []
    (let [nations (subscribe [:all-nations])]
      [:div#fake-map
       [:div
        {:on-click #(dispatch [:select-blank])}
        "(Blank Space)"]
       (for [nation @nations]
         [:div {:on-mouse #(dispatch [:focus-nation nation])
                :on-click #(dispatch [:select-nation nation])
                :key nation}
          nation
          [:br]])
       [:div
        {:on-click #(dispatch [:select-blank])}
        "(Blank Space)"]])))

(defn wmtm-app
  []
  (let [track-titles (subscribe [:selected-composition-titles])]
    (fn []
      [:div
       [dummy-nation-list]
       [:div#fake-track-list
        [:ul
         (for [title @track-titles]
           [:li {:key title} title])]]])))
