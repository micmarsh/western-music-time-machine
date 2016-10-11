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
In order to initially capture the actions listed above with minimal regard for the specific look and feel of things, I jumped into re-frame to build something contained all the the core interactions, but without displaying a map or actually playing any kind of music (yet). This, combined with re-frame's already optioninated way of splitting up data, event handlers, subscriptions, and views, would ensure good abstraction between the different layers of the application.

#### Re-Frame Observations
(THIS SHIT RIGHT HERE IS GETTING REFACTORED, WRITE EVENT HANDLERS SECTION AS IF NONE OF THIS EVER HAPPENDED)
Speaking of abstraction, as I worked through this (and later) stages of the project, I started to notice a few trends in the way re-frame's abstractions shook out in practice.

I quickly learned that it didn't make very much sense to keep logic in subscriptions: if there was some interaction between pieces of the global state of data that was mattered for the way things were ultimately displayed, it nearly always made sense to move that logic to the handler/library function (more on the latter soon), and make the subscription a thin wrapper over some piece of data.
(LOL YOU'VE GOT SOMETHING DIRECTLY REBELLING AGAINST THIS IN THE MAIN SUBS FILE RIGHT NOW. PERHAPS SHOULD GO BACK AND CLEAN THAT SHIT UP. COULD EVEN WRITE ABOUT EXACTLY THAT PROCESS HERE WHEN DONE)

While the [explaination of subscriptions (at the time)](BETTER REFER TO TUTUORIAL TO BACK UP THIS ACCUSATION YO), talks about FRP and data transformation, in practice the proper role of subscriptions appeared to be preparing data to to be utilized by views, with minimal fuss otherwise. After all, no reason to "fmap a function over a Signal" (to badly paraphrase old Elm terminology) to do something when you can accomplish the same thing with a normal looking (data in -> new data out) function.
(ACKNOWLEDGE POSSIBLE BENEFITS FOR ASYNC PROPOGATION OF THINGS HERE?????)

On the topic of moving things logic into more suitable places, I also realized that event handlers should contain a similiarly minimimal amount of logic (wow, Single Responsiblilty Principle, who would have guessed, right?). (NOT EVEN SURE WHAT ELSE TO SAY HERE, REFERENCE ASYNC EVEN THOUGH NOT YET MENTIONED ABOVE)
(PERHAPS THIS AND THE ABOVE NEED SOME GODDAMN BEFORE AND AFTERS. NAH, IT'S PRETTY SIMPLE (AND OBVIOUS), PERHAPS JUST USE AS TRANSITION TO TALKING ABOUT SPEC)
LEGIT SHIT:

As hands-on experience building the application led to an appreciattion of the concepts of re-frame, the benefits of purely functional event handlers became clear. I was able to move all logic out of event handlers in to a separate library function. This had the immediate benefit of being able to hide the ever-shifting structure of the underlying global db object from event handlers, and made the future implemention of testing a breeze. This was even before the re-frame 0.8 and its effects system formalizing and "purifying" all side-effects.

### A Note on Functional Purity
Several years ago now, I went through a (likely motivated by my day-to-day work in JavaScript) phase of intense enthusiasm for functional programming, or something we'll call "Haskell Worship". Haskell's pure IO model was the be-all end-all gold standard of programming, and the quality of any particular piece of software was directly correlated with how closely its implemenation approached that ideal. Of course, I never would have phrased it that way at the time, but that was basically the unconscious thought process.

While some good things undoubtedly came out of that phase (such as the learned concepts expressed in [this particular project I'll shamelessly shill for here](https://github.com/micmarsh/clojure-pure-io)), it took working on a much larger scale software project to realize that "purifying every last side effect" was very impractical, and thus counter-productive in many contexts. I'm still firmly of the opinion that things like persistent data structures and pure functions are great, but ultimately exist in the service of some broader overall software architecture.

I felt this worth noting before continuing on to the next section, because I'm every excited about re-frames pure effects system and its monadic potential, but don't want to come off as advocating such things being great for their own sake, in the style of Haskell Worship.

### Integrating Key Third-Party Pieces



