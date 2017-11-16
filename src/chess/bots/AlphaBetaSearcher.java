package chess.bots;

import java.util.List;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

public class AlphaBetaSearcher<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {

    public M getBestMove(B board, int myTime, int opTime) {
        /* Calculate the best move */
        BestMove<M> best = alphabeta(this.evaluator, board, ply, -evaluator.infty(),
                evaluator.infty());

        // Update display of our best move:
        this.reportNewBestMove(best.move);

        return best.move;
    }
    
    public BestMove<M> evalBaseCase(Evaluator<B> evaluator, B board, int depth) {
        // No moves.

        if (board.inCheck()) {
            return new BestMove<M>(null, -evaluator.mate() - depth);
        } else {
            return new BestMove<M>(null, -evaluator.stalemate());
        }
    }

    public BestMove<M> alphabeta(Evaluator<B> evaluator, B board, int depth,
            int alpha, int beta) {

        if (depth == 0) {
            return new BestMove<M>(null, evaluator.eval(board));
        }

        List<M> moves = board.generateMoves();

        if (moves.isEmpty()) {
            return evalBaseCase(evaluator, board, depth);
        }

        BestMove<M> alphaMove = new BestMove<M>(null, alpha);

        for (M move : moves) {
            board.applyMove(move);

            BestMove<M> bestSubmove = alphabeta(evaluator, board, depth - 1, -beta, -alpha).negate();

            board.undoMove();

            if (bestSubmove.value > alpha) {
                alpha = bestSubmove.value;
                // Note the move that beat the prior one:
                alphaMove.move = move;
                alphaMove.value = alpha;
            }

            if (alpha >= beta) {
                return alphaMove;
            }
        }

        return alphaMove;
    }
}