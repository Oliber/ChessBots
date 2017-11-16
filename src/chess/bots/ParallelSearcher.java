package chess.bots;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

public class ParallelSearcher<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {

    public static final ForkJoinPool POOL = new ForkJoinPool();

    public static final int DIVIDE_CUTOFF = 3;

    public M getBestMove(B board, int myTime, int opTime) {
        BestMove<M> best = POOL.invoke(new EvalBoardTask(board, ply, cutoff, null));

        // Update display of our best move:
        this.reportNewBestMove(best.move);

        return best.move;
    }

    @SuppressWarnings("serial")
    public class EvalBoardTask extends RecursiveTask<BestMove<M>> {
        public B board;
        public int depth;
        public int cutoff;
        public M move;

        public EvalBoardTask(B board, int depth, int cutoff, M move) {
            this.board = board;
            this.depth = depth;
            this.cutoff = cutoff;
            this.move = move;
        }

        @Override
        protected BestMove<M> compute() {

            if (depth == cutoff) {
                return SimpleSearcher.minimax(evaluator, board, depth);
            }

            // Copied from SimpleSearcher
            List<M> moves = board.generateMoves();

            if (moves.isEmpty()) {
                // No moves.

                if (board.inCheck()) {
                    return new BestMove<M>(null, -evaluator.mate() - depth);
                } else {
                    return new BestMove<M>(null, -evaluator.stalemate());
                }
            }

            DivideTask divide = new DivideTask(this.board, depth, evaluator, cutoff, 0, moves.size(), moves);
            return divide.compute();
        }
    }

    @SuppressWarnings("serial")
    public class DivideTask extends RecursiveTask<BestMove<M>> {
        public B board;
        public int depth, cutoff;
        public List<M> moves;
        public int lo, hi;

        public Evaluator<B> evaluator;

        public DivideTask(B board, int depth, Evaluator<B> evaluator, int cutoff, int lo, int hi, List<M> moves) {
            this.board = board;
            this.depth = depth;
            this.evaluator = evaluator;
            this.cutoff = cutoff;
            this.lo = lo;
            this.hi = hi;
            this.moves = moves;
        }

        @Override
        protected BestMove<M> compute() {

            if (hi - lo <= DIVIDE_CUTOFF) {

                int length = hi - lo;
                @SuppressWarnings("unchecked")
                EvalBoardTask[] tasks = (EvalBoardTask[]) Array.newInstance(EvalBoardTask.class, length);

                int j = 0;

                for (int i = lo; i < hi; i++) {
                    M move = this.moves.get(i);
                    this.board.applyMove(move);

                    EvalBoardTask task = new EvalBoardTask(board.copy(), depth - 1, cutoff, move);
                    task.fork();
                    tasks[j++] = task;

                    this.board.undoMove();
                }

                BestMove<M> bestMove = new BestMove<M>(null, -evaluator.infty());

                // Traverse through to get the best
                for (int k = 0; k < tasks.length; k++) {

                    BestMove<M> move = tasks[k].join().negate();

                    if (move.value > bestMove.value) {
                        bestMove = move;
                        bestMove.move = tasks[k].move;
                    }
                }

                return bestMove;
            }

            int mid = lo + (hi - lo) / 2;

            DivideTask left = new DivideTask(this.board.copy(), depth, evaluator, cutoff, lo, mid, moves);
            DivideTask right = new DivideTask(this.board.copy(), depth, evaluator, cutoff, mid, hi, moves);

            right.fork();

            BestMove<M> leftMove = left.compute();
            BestMove<M> rightMove = right.join();

            if (leftMove.value > rightMove.value) {
                return leftMove;
            } else {
                return rightMove;
            }
        }
    }
}