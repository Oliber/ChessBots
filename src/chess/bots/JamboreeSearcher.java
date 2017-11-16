package chess.bots;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Move;

/**
 * Implements parallel alpha-beta.
 */
public class JamboreeSearcher<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {
    
    public static final ForkJoinPool POOL = new ForkJoinPool();

    public M getBestMove(B board, int myTime, int opTime) {
        BestMove<M> best = POOL.invoke(
                new EvalBoardTask(board, ply, null, -evaluator.infty(), evaluator.infty()));

        this.reportNewBestMove(best.move);

        return best.move;
    }

    private class EvalBoardTask extends RecursiveTask<BestMove<M>> {
        private final B board;
        private final int depth;
        private final int alpha;
        private final int beta;
        private M move;

        public EvalBoardTask(B board, int depth, M move, int alpha, int beta) {
            this.board = board;
            this.depth = depth;
            this.move = move;
            this.alpha = alpha;
            this.beta = beta;
        }

        @Override
        protected BestMove<M> compute() {
            if (depth == JamboreeSearcher.this.cutoff) {
                // Finish with sequential alphabeta.
                AlphaBetaSearcher<M, B> alphaBetaSearcher =
                        new AlphaBetaSearcher<M, B>();
                return alphaBetaSearcher.alphabeta(
                        evaluator, board, depth, alpha, beta);
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

            DivideTask divide = new DivideTask(
                    this.board, depth, 0, moves.size(), moves, alpha, beta);
            return divide.compute();
        }
    }

    @SuppressWarnings("serial")
    private class DivideTask extends RecursiveTask<BestMove<M>> {
        private static final int DIVIDE_CUTOFF = 6;
        private static final float PERCENTAGE_CUTOFF = 0.45f;

        private B board;
        private final int depth;
        private final int lo, hi;
        private final List<M> moves;
        private int alpha;
        private final int beta;

        public DivideTask(B board, int depth,
                int lo, int hi, List<M> moves, int alpha, int beta) {
            this.board = board;
            this.depth = depth;
            this.lo = lo;
            this.hi = hi;
            this.moves = moves;
            this.alpha = alpha;
            this.beta = beta;
        }

        @Override
        protected BestMove<M> compute() {

            this.board = this.board.copy();
            int length = hi - lo;

            if (length <= DIVIDE_CUTOFF) {

                // Subarray is small enough to conquer.

                @SuppressWarnings("unchecked")
                EvalBoardTask[] tasks =
                 (EvalBoardTask[]) Array.newInstance(EvalBoardTask.class, length);

                int j = 0;

                for (int i = lo; i < hi; i++) {
                    M move = this.moves.get(i);
                    this.board.applyMove(move);

                    EvalBoardTask task = new EvalBoardTask(
                            board.copy(), depth - 1, move, -beta, -alpha);
                    task.fork();
                    tasks[j++] = task;

                    this.board.undoMove();
                }

                BestMove<M> bestMove = new BestMove<M>(null, -evaluator.infty());

                // Traverse through, join, and get the best.
                for (int k = 0; k < tasks.length; k++) {
                    BestMove<M> move = tasks[k].join().negate();

                    if (move.value > alpha) {
                        alpha = move.value;
                        bestMove = move;
                        bestMove.move = tasks[k].move;
                    }
                    
                    if (alpha >= beta) {
                        return bestMove;
                    }
                }

                return bestMove;
            }

            // Otherwise, we do X% of the moves sequentially and the rest in parallel.

            int sequentialCount = (int) (PERCENTAGE_CUTOFF * length);
            int sequentialLo = lo + sequentialCount;
            BestMove<M> bestMove = new BestMove<M>(null, alpha);

            for (int i = lo; i < sequentialLo; i++) {
                M move = moves.get(i);
                board.applyMove(move);
                EvalBoardTask evalTask = new EvalBoardTask(
                        board, depth - 1, move, -beta, -alpha);
                BestMove<M> bestSubmove = evalTask.compute().negate();
                board.undoMove();

                if (bestSubmove.value > alpha) {
                    alpha = bestSubmove.value;
                    bestMove = bestSubmove;
                    bestMove.move = move;
                }
                
                if (alpha >= beta) {
                    return bestMove;
                }
            }

            int mid = sequentialLo + (hi - sequentialLo) / 2;

            DivideTask left = new DivideTask(
                    this.board, depth, sequentialLo, mid, moves, alpha, beta);

            left.fork();

            DivideTask right = new DivideTask(
                    this.board, depth, mid, hi, moves, alpha, beta);

            BestMove<M> rightMove = right.compute();
            BestMove<M> leftMove = left.join();
            
            if (leftMove.value >= rightMove.value) {
                if (leftMove.value > bestMove.value) {
                    return leftMove;
                }
            }
            else {
                if (rightMove.value > bestMove.value) {
                    return rightMove;
                }
            }
            
            return bestMove;
        }
    }
}