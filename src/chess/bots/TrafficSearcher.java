package chess.bots;

import chess.bots.AlphaBetaSearcher;
import chess.bots.BestMove;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

public class TrafficSearcher<M extends Move<M>, B extends Board<M, B>> extends
        AlphaBetaSearcher<M, B> {

    @Override
    public BestMove<M> evalBaseCase(Evaluator<B> evaluator, B board, int depth) {
        // Override base case -- there is no stalemate/mate/etc in traffic.
        return new BestMove<M>(evaluator.eval(board));
    }
}