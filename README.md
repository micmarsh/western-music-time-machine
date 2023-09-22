# Western Music Time Machine

A celebration of timeless creativity through distinctly modern creative means. Originated as a way to learn [Re-Frame](https://github.com/day8/re-frame) by creating something non-trivial, but is still functional enough for discovering and listening to actual music.

![Screenshot of application in action](https://github.com/micmarsh/techcomm/blob/master/alternative-view-small.png)

## Using This Repostitory

`lein figwheel` runs the main application

### Updating the Data Set
All of the data used for the application lives in the static file [compositions.edn](https://github.com/micmarsh/western-music-time-machine/blob/master/resources/public/edn/compositions.edn). 

To automatically add to this file, run the function contained in the `comment` in [track.clj](https://github.com/micmarsh/western-music-time-machine/blob/master/src/clj/western_music/ingest/track.clj), which will search YouTube for the desired trackname and artist and update the file with the result (assuming a valid YouTube API key).

Any other edits can be done manually to the file, although realistically better update and deletion tools need to be written. 

## More Information
 I used this project as the subject of [a presentation](http://micmarsh.github.io/techcomm) for a Technical Communication class when I finished my last semester of undergrad. 
 
 Although the actual video and notes for the presentation have been lost, the bullet points in the presentation should give some insight into the motivations and design process.

## TODO
* Several tracks that refer to obsolete video urls that will need updating or deleting.

## Credits
Special thanks to http://freehtml5maps.com/free-html5-europe-map/ for a lot of work
