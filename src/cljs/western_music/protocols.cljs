(ns western-music.protocols)

(defprotocol DisplayData
  (display [data])
  (id [data]))

(extend-protocol DisplayData
  string
  (display [s] s)
  (id [s] s))
