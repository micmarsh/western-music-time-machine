(ns western-music.dev
  (:require [western-music.core :refer [main]]
            [figwheel.client :as fw]))

(fw/start {:on-jsload main
           :websocket-url "ws://localhost:3450/figwheel-ws"})
