(ns western-music.protocols)

(defprotocol DisplayData
  (display [data])
  (id [data]))

(extend-protocol DisplayData
  string
  ;; This is acting way weird, hopefull it's just a console/printing issue
  (display [s] s)
  (id [s] s)
  nil
  (display [_])
  (id [_]))
