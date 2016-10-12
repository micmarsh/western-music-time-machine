# Blah Blah Title

Ever since gaining an appreciation for classical music, I've been kicking around this idea of a project to, in my own small way, honor the greats that came before. Recently finding myself with a bit of time on my hands and a desire to work with ClojureScript in a non-trivial way, I knew it was time to pull the trigger.

## Pre Work

### The Data
Since this is the kind of application that (currently) relies on its own dataset rather than user-generated content,  I knew it would be difficult to do anything without acquiring such data. The first order of business, then, was to define a suitable target format and source of the desired data.

I had initially wanted to somehow organize application around fine-grained time and place data connected to the compositions themselves: a user could track a given composer's travels via the composition place and time of each of their tracks, or get a sense of the cultural moment in different cities or nations via which tracks were performed there at particular dates.

As it turns out, such specific data about different compositions is very difficult to acquire. While one can typically glean it from reading a piece's Wikipedia page (for example, that [Beethoven's 7th](https://en.wikipedia.org/wiki/Symphony_No._7_(Beethoven)) was first performed in Vienna in 1813), it would require some sophisticated (at least by the standards of this project/my knowledge) NLP work to extract that data about different tracks in a generic and automated way.

Instead, I settled on only acquiring composers' birth place and years, which was widely available from a variety of sources (I settled on [Wikidata](link to code?), for the relative lack of licensing requirements), and roughly organizing the data (and application) in the hierarchy: nations contain composers contain compositions. The ignoring of the time based element (the birth year of composers) was primiarly due to the small size of the data in general, where filtering by time period would reduce the already limited list of compositions.

### The Design
The next priority, as it should be with any project intended to be viewd by more than one person, was to come up with a design (in the holistic sense) based on a set of end-user requirements. That perhaps should have been the very first thing (before the data stuff above), but such was the way my internal thought process went at the time. In this case, I was a user, product owner, project manager, and developer all at once, so it was extra important to settle on something narrow and strict enough to guide development in a productive direction.

After a few incredibly crude sketches of a sort of UI, I decided to define each important piece of said UI as a user action, and said actions would serve as the hard requirements moving forward.
* selecting a nation and generating a list of composers born in that nation
* selectingg a composer and generating tracks written by said composer
* enqueue a given track
* playing and pausing the current track
* moving backwards and forwards between tracks in the queue
Once the above was layed out, it time to get into the implemenation

## The Application

### Framework Choice
While The Great and Mighty ClojureScript was clearly the proper language choice for this and any other front-end application, the choice of what framework to use was still up in the air. My limited experience playing around in the ecosystem prior made me aware of the simplicity and power the React paradigm could provide, particularly in the context of functional programming, and that the major split there was Reagent vs. Om.

For whatever reason, the "Object-Oriented" feel of Om had always rubbed me the wrong way. Having to implement particular interfaces just *felt* less simple than manipulating hiccup data structures. I knew there were very good reasons that Om worked the way it did, related to encapsulation and better structuring of very large codebases, but at the end of the day, my personal aesthetic taste won out and Reagent was in.

Soon after that decision, I came across the [re-frame](dat link) framework, and boy, am I glad that I did. Although the README and associated documents on its GitHub page speak for themselves and do it way more justice than I ever could here, needless to say I was thoroughly impressed by everything it offered. This was before the current iteration, too (0.8 as of this writing), which we'll get to in a bit.

### The First Iteration
Using a sort of [Tracer Bullet Appoarch](https://stackoverflow.com/questions/743815/tracer-bullet-development) (with myself as the "client"), I decided to implement an unstyled skeleton application that covered all the bases of selecting tracks, playing tracks, and manipulating the play queue, everything short of actually playing real audio.

### Integrating Key Third-Party Pieces
(TODO: THIS. MAY NOT EVEN WARRANT ITS OWN HEADER)

As hands-on experience building the application led to an appreciattion of the concepts of re-frame, the benefits of purely functional event handlers became clear. I was able to move all logic out of event handlers in to a separate library function. This had the immediate benefit of being able to hide the ever-shifting structure of the underlying global db object from event handlers, and made the future implemention of testing a breeze. This was even before the re-frame 0.8 and its effects system formalizing and "purifying" all side-effects.

### A Note on Functional Purity
Several years ago now, I went through a (likely motivated by my day-to-day work in JavaScript) phase of intense enthusiasm for functional programming, or something we'll call "Haskell Worship". Haskell's pure IO model was the be-all end-all gold standard of programming, and the quality of any particular piece of software was directly correlated with how closely its implemenation approached that ideal. Of course, I never would have phrased it that way at the time, but that was basically the unconscious thought process.

While some good ideas undoubtedly came out of that phase, it took working on a much larger scale software project to realize that "purifying every last side effect" was very impractical, and thus counter-productive in many contexts. I'm still firmly of the opinion that things like persistent data structures and pure functions are great, but ultimately exist in the service of some broader overall software architecture.

I felt this worth noting before continuing on to the next section, because I'm every excited about re-frames pure effects system and its monadic potential, but don't want to come off as advocating such things being great for their own sake, in the style of Haskell Worship.

### The Great Functional Re-Write
Sometime after the application was in a more or less usuable place, [re-frame 0.8](LINK YO) was released, gracefully extending previously database-only event handlers using the concept of "effects". Now, an event means some number of effects will happen, which could be a database update, another event dispatch, or any number of other, endlessly extensible things.

(MAY MENTION THIS ABOVE/IN "MUNDANE" SECTION) While this was a welcome step a more functional direction, I was left wondering about how best to refactor the [event dispatches littered throughout the otherwise pure UI data library](LINK TO AN EXAMPLE OF THIS HERE BUSINESS). While perhaps a more fundamental re-write was in order, I eventually stumbled upon (what I believe to be) a very elegant solution for leaving most of the existing logic in place: a "database + events" monad a'la Haskell or PureScript.

If you've already got a picture in your head of what I'm talking about, I encourage you to read on. Otherwise, I humbly submit [an old project of mine](https://github.com/micmarsh/clojure-pure-io/blob/master/gist.md) as a starting point for learning about such things (it also contains links to more serious sources, as well)

Similar to the way Haskell IO's `bind` "appends" a new effect onto its old value, this `bind` both replaces the old database value and appends any provided event dispatches to whatever dispatches existed in the old value. If the database map has type `db`, and we call a full re-frame fx map `Eff`, then `bind` has the signature `db -> Eff db`, only having access to a database value, returning a new monad that occasionally contains a description of an event to dispatch.

[You can find the code here](LINK YO). I think this could be really big for re-frame, but want to find a good way to generalize and open the way `:dispatch` merging works for extension. The current implemenation is perfect for my use, but the infinite extensibiliy of effects themselves deserve similar extensibility in the way their monad works.

