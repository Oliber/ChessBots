package chess.bots;

import java.util.List;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

/**
 * This class should implement the minimax algorithm as described in the
 * assignment handouts.
 */
public class SimpleSearcher<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {

    public M getBestMove(B board, int myTime, int opTime) {
        /* Calculate the best move */
        BestMove<M> best = SimpleSearcher.minimax(this.evaluator, board, ply);

        // Update display of our best move:
        this.reportNewBestMove(best.move);

        return best.move;
    }

    static <M extends Move<M>, B extends Board<M, B>> BestMove<M> minimax(Evaluator<B> evaluator, B board, int depth) {

        if (depth == 0) {
            return new BestMove<M>(null, evaluator.eval(board));
        }

        List<M> moves = board.generateMoves();

        if (moves.isEmpty()) {
            // No moves.

            if (board.inCheck()) {
                return new BestMove<M>(null, -evaluator.mate() - depth);
            } else {
                return new BestMove<M>(null, -evaluator.stalemate());
            }
        }

        BestMove<M> bestMove = new BestMove<M>(null, -evaluator.infty());

        for (M move : moves) {
            board.applyMove(move);

            BestMove<M> bestSubmove = minimax(evaluator, board, depth - 1).negate();

            board.undoMove();

            if (bestSubmove.value > bestMove.value) {
                bestMove = bestSubmove;
                // Note the move that beat the prior one:
                bestMove.move = move;
            }
        }

        return bestMove;
    }
}