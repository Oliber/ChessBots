# Project 3 (Chess) Feedback #
## CSE 332 Winter 2016 ##

**Team:** David Grant (dagr) and Allen Tran (alltran) <br />
**Graded By:** Ollin (ollin@cs.washington.edu)
<br>

## Unit Tests ##

**Minimax**  `(4/4)`
> ✓ Passed *depth2* <br>
> ✓ Passed *depth3* <br>

**ParallelMinimax**  `(15/15)`
> ✓ Passed *depth2* <br>
> ✓ Passed *depth3* <br>
> ✓ Passed *depth4* <br>

**AlphaBeta**  `(9/9)`
> ✓ Passed *depth2* <br>
> ✓ Passed *depth3* <br>
> ✓ Passed *depth4* <br>

**Jamboree**  `(20/20)`
> ✓ Passed *depth2* <br>
> ✓ Passed *depth3* <br>
> ✓ Passed *depth4* <br>
> ✓ Passed *depth5* <br>

## Clamps Tests ##

*Score*
`(8.0/8)`

--------


## Optimal Parallelism ##

Great job on your code! We found a few problems though with your parallelism code that would definitely slow it down.

** ParallelSearcher **

`(-3/0)` Copied in Parent instead of in Child <br />
`(-2/0)` Doesn't Compute Last Thread instead of Forking It <br />

** JamboreeSearcher **

`(-3/0)` Copied in Parent instead of in Child <br />
`(-2/0)` Doesn't Compute Last Thread instead of Forking It <br />


--------

## Write-Up ##

**Project Enjoyment**
`(3/3)`

It's good to hear you enjoyed this project!  And your feedback is appreciated.

**Chess Server**
`(2/2)`

### Experiments ###

**Chess Game**
`(6/6)`

Really nice charts here!

**Sequential Cut-Offs**
`(7/7)`

**Number of Processors**
`(7/7)`

**Comparing the Algorithms**
`(7/8)`

Your table doesn't have units.  I was able to infer it from the subsequent chart, but please try to include units in the table as well for clarity!

### Traffic ###

**Beating Traffic**
`(6/6)`

### Above and Beyond ###

**Above and Beyond**
`(EX: 4)` for flexo games
