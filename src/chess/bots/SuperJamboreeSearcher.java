package chess.bots;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Move;

/**
 * Implements parallel alpha-beta.
 */
public class SuperJamboreeSearcher<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {

    public static final ForkJoinPool POOL = new ForkJoinPool();
    public static final int SEQUENTIAL_CUTOFF = 6;

    // Contains information pertaining to the move.
    public class MoveInfo {
        public M move;
        public int val;
        
        public MoveInfo(M move, B board) {
            this.move = move;
            board.applyMove(move);
            this.val = evaluator.eval(board);
            board.undoMove();
        }
    }
    
    
    
    
    public M getBestMove(B board, int myTime, int opTime) {
        long t = System.currentTimeMillis();
        long t2;
        int depth = 0;
        final long TIME_BUDGET = 800;
        BestMove<M> best = null;

        do {
            ++depth;
            
            best = POOL.invoke(new EvalBoardTask(board, depth,
                    null, -evaluator.infty(), evaluator.infty(), best == null ? null : best.move));

            t2 = System.currentTimeMillis() - t;
            System.out.format(" >>> best : %s, depth : %d/%d, time : %d,  value : %d\n", best.move.toString(), depth, ply, t2, best.value);

        } while (t2 < TIME_BUDGET && depth <= ply+1);

        this.reportNewBestMove(best.move);
        System.out.format("Best Move : %s Value : %d depth=%d\n",
                best.move.toString(), best.value, depth);
        return best.move;
    }

    @SuppressWarnings("serial")
    private class EvalBoardTask extends RecursiveTask<BestMove<M>> {
        private B board;
        private int depth;
        private int alpha;
        private int beta;
        private M move;
        private M previousBestMove;
        
        // Simple sort of the moves
        public class MoveComparator implements Comparator<MoveInfo> {
            @Override
            public int compare(MoveInfo m1, MoveInfo m2) {
                if (EvalBoardTask.this.previousBestMove != null) {
                    if (m1.move.equals(EvalBoardTask.this.previousBestMove)) {
                        return -1;
                    }
                    if (m2.move.equals(EvalBoardTask.this.previousBestMove)) {
                        return 1;
                    }
                }

                if(m1.val < m2.val) return 1;
                if(m1.val > m2.val) return -1;
                return 0;
            }
        }

        public EvalBoardTask(B board, int depth, M move, int alpha, int beta) {
            this(board, depth, move, alpha, beta, null);
        }
        public EvalBoardTask(B board, int depth, M move, int alpha, int beta, M previousBestMove) {
            this.board = board;
            this.depth = depth;
            this.move = move;
            this.alpha = alpha;
            this.beta = beta;
            this.previousBestMove = previousBestMove;
        }

        @Override
        protected BestMove<M> compute() {
            if (depth <= cutoff) {
                // Perform sequential alphabeta.
                AlphaBetaSearcher<M, B> alphaBetaSearcher =
                        new AlphaBetaSearcher<M, B>();
                return alphaBetaSearcher.alphabeta(
                        evaluator, board, depth, alpha, beta);
            }

            List<MoveInfo> moves = convertToWrapper(board.generateMoves(), this.board);
            
            if (previousBestMove != null) {
                assert(moves.get(0).move.equals(previousBestMove));
            }

            if (moves.isEmpty()) return lastMove(board);
            
            DivideTask divide = new DivideTask(this.board, depth,
                    0, moves.size(), moves, alpha, beta);
            return divide.compute();
        }

        private BestMove<M> lastMove(B board2) {
            if (board2.inCheck()) {
                return new BestMove<M>(null, -evaluator.mate() - depth);
            } else {
                return new BestMove<M>(null, -evaluator.stalemate());
            }
        }

        private List<MoveInfo> convertToWrapper(List<M> generateMoves, B board2) {
            List<MoveInfo> list = new ArrayList<MoveInfo>();
            for(M m : generateMoves) {
                list.add(new MoveInfo(m, board2));
            }
            list.sort(new MoveComparator());
            return list;
        }
    }

    @SuppressWarnings("serial")
    private class DivideTask extends RecursiveTask<BestMove<M>> {
        private static final float PERCENTAGE_CUTOFF = 0.10f;

        private B board;
        private int depth;
        private int lo, hi;
        private List<MoveInfo> moves;
        private int alpha, beta;

        public DivideTask(B board, int depth, int lo, int hi, List<MoveInfo> moves,
                int alpha, int beta) {
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

            // Subarray is small enough to conquer.
            if (length <= SEQUENTIAL_CUTOFF) return sequential(length);

            // X% of the moves sequentially
            int sequentialCount = (int) Math.ceil(PERCENTAGE_CUTOFF * length);
            int sequentialLo = lo + sequentialCount;
            BestMove<M> bestMove = new BestMove<M>(null, alpha);

            for (int i = lo; i < sequentialLo; i++) {
                M move = moves.get(i).move;
                board.applyMove(move);
                EvalBoardTask evalTask = new EvalBoardTask(board, depth - 1, move, -beta, -alpha);
                BestMove<M> bestSubmove = evalTask.compute().negate();
                board.undoMove();

                bestMove = updateAlpha(bestSubmove, bestMove, move);
                if (alpha >= beta) return bestMove;
            }
            // The rest in parallel
            int mid = sequentialLo + (hi - sequentialLo) / 2;

            DivideTask left = new DivideTask(this.board, depth, sequentialLo, mid, moves, alpha, beta);

            left.fork();

            DivideTask right = new DivideTask(this.board, depth, mid, hi, moves, alpha, beta);

            BestMove<M> rightMove = right.compute();
            BestMove<M> leftMove = left.join();

            return nextMove(leftMove, rightMove, bestMove);
        }

        private BestMove<M> nextMove(BestMove<M> leftMove, BestMove<M> rightMove, BestMove<M> bestMove) {
            if (leftMove.value >= rightMove.value) {
                if (leftMove.value > bestMove.value)
                    return leftMove;
            } else {
                if (rightMove.value > bestMove.value)
                    return rightMove;
            }
            return bestMove;
        }

        private BestMove<M> sequential(int length) {
            @SuppressWarnings("unchecked")
            EvalBoardTask[] tasks = (EvalBoardTask[]) Array.newInstance(EvalBoardTask.class, length);

            int j = 0;

            for (int i = lo; i < hi; i++) {
                M move = this.moves.get(i).move;
                this.board.applyMove(move);

                EvalBoardTask task = new EvalBoardTask(board.copy(), depth - 1, move, -beta, -alpha);
                task.fork();
                tasks[j++] = task;

                this.board.undoMove();
            }

            BestMove<M> bestMove = new BestMove<M>(null, -evaluator.infty());

            // Traverse through, join, and get the best.
            for (int k = 0; k < tasks.length; k++) {
                BestMove<M> move = tasks[k].join().negate();

                bestMove = updateAlpha(move, bestMove, tasks[k].move);
                if (alpha >= beta) return bestMove;
            }

            return bestMove;
        }

        private BestMove<M> updateAlpha(BestMove<M> bestSubmove, BestMove<M> bestMove, M move) {
            if (bestSubmove.value > alpha) {
                alpha = bestSubmove.value;
                bestMove = bestSubmove;
                bestMove.move = move;
            }
            return bestMove;
        }
    }
}