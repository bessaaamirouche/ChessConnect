/**
 * myChessBot - Simple Chess Engine for Web Workers
 * A lightweight chess engine that doesn't require WASM
 */

let skillLevel = 10;

const PIECE_VALUES = {
  'p': 100, 'n': 320, 'b': 330, 'r': 500, 'q': 900, 'k': 20000,
  'P': -100, 'N': -320, 'B': -330, 'R': -500, 'Q': -900, 'K': -20000
};

function parseFEN(fen) {
  const parts = fen.split(' ');
  const board = [];
  const rows = parts[0].split('/');
  for (const row of rows) {
    const boardRow = [];
    for (const char of row) {
      if (char >= '1' && char <= '8') {
        for (let i = 0; i < parseInt(char); i++) boardRow.push(null);
      } else {
        boardRow.push(char);
      }
    }
    board.push(boardRow);
  }
  return { board, turn: parts[1] || 'w', castling: parts[2] || '-', enPassant: parts[3] || '-' };
}

function toAlgebraic(row, col) { return String.fromCharCode(97 + col) + (8 - row); }
function fromAlgebraic(sq) { return { row: 8 - parseInt(sq[1]), col: sq.charCodeAt(0) - 97 }; }
function isWhite(p) { return p && p === p.toUpperCase(); }
function isBlack(p) { return p && p === p.toLowerCase(); }

function generateMoves(state) {
  const moves = [];
  const { board, turn } = state;
  const isW = turn === 'w';
  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const p = board[r][c];
      if (!p || (isW && !isWhite(p)) || (!isW && !isBlack(p))) continue;
      const from = toAlgebraic(r, c);
      const pt = p.toLowerCase();
      if (pt === 'p') addPawnMoves(moves, board, r, c, isW, from, state.enPassant);
      else if (pt === 'n') addKnightMoves(moves, board, r, c, isW, from);
      else if (pt === 'b') addSliding(moves, board, r, c, isW, from, [[-1,-1],[-1,1],[1,-1],[1,1]]);
      else if (pt === 'r') addSliding(moves, board, r, c, isW, from, [[-1,0],[1,0],[0,-1],[0,1]]);
      else if (pt === 'q') { addSliding(moves, board, r, c, isW, from, [[-1,-1],[-1,1],[1,-1],[1,1]]); addSliding(moves, board, r, c, isW, from, [[-1,0],[1,0],[0,-1],[0,1]]); }
      else if (pt === 'k') addKingMoves(moves, board, r, c, isW, from, state.castling);
    }
  }
  return moves;
}

function addPawnMoves(moves, board, r, c, isW, from, ep) {
  const dir = isW ? -1 : 1, start = isW ? 6 : 1, promo = isW ? 0 : 7;
  const nr = r + dir;
  if (nr >= 0 && nr < 8 && !board[nr][c]) {
    const to = toAlgebraic(nr, c);
    moves.push(nr === promo ? from + to + 'q' : from + to);
    if (r === start && !board[r + 2 * dir][c]) moves.push(from + toAlgebraic(r + 2 * dir, c));
  }
  for (const dc of [-1, 1]) {
    const nc = c + dc;
    if (nc >= 0 && nc < 8 && nr >= 0 && nr < 8) {
      const t = board[nr][nc], epSq = toAlgebraic(nr, nc);
      if ((t && (isW ? isBlack(t) : isWhite(t))) || epSq === ep) {
        const to = toAlgebraic(nr, nc);
        moves.push(nr === promo ? from + to + 'q' : from + to);
      }
    }
  }
}

function addKnightMoves(moves, board, r, c, isW, from) {
  for (const [dr, dc] of [[-2,-1],[-2,1],[-1,-2],[-1,2],[1,-2],[1,2],[2,-1],[2,1]]) {
    const nr = r + dr, nc = c + dc;
    if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
      const t = board[nr][nc];
      if (!t || (isW ? isBlack(t) : isWhite(t))) moves.push(from + toAlgebraic(nr, nc));
    }
  }
}

function addSliding(moves, board, r, c, isW, from, dirs) {
  for (const [dr, dc] of dirs) {
    let nr = r + dr, nc = c + dc;
    while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
      const t = board[nr][nc];
      if (!t) { moves.push(from + toAlgebraic(nr, nc)); }
      else { if (isW ? isBlack(t) : isWhite(t)) moves.push(from + toAlgebraic(nr, nc)); break; }
      nr += dr; nc += dc;
    }
  }
}

function addKingMoves(moves, board, r, c, isW, from, castling) {
  for (const [dr, dc] of [[-1,-1],[-1,0],[-1,1],[0,-1],[0,1],[1,-1],[1,0],[1,1]]) {
    const nr = r + dr, nc = c + dc;
    if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
      const t = board[nr][nc];
      if (!t || (isW ? isBlack(t) : isWhite(t))) moves.push(from + toAlgebraic(nr, nc));
    }
  }
  if (isW && r === 7 && c === 4) {
    if (castling.includes('K') && !board[7][5] && !board[7][6]) moves.push('e1g1');
    if (castling.includes('Q') && !board[7][3] && !board[7][2] && !board[7][1]) moves.push('e1c1');
  } else if (!isW && r === 0 && c === 4) {
    if (castling.includes('k') && !board[0][5] && !board[0][6]) moves.push('e8g8');
    if (castling.includes('q') && !board[0][3] && !board[0][2] && !board[0][1]) moves.push('e8c8');
  }
}

function evaluate(state) {
  let score = 0;
  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const p = state.board[r][c];
      if (p) {
        score += PIECE_VALUES[p] || 0;
        if (c >= 2 && c <= 5 && r >= 2 && r <= 5) score += isWhite(p) ? -10 : 10;
      }
    }
  }
  return score;
}

function makeMove(state, move) {
  const newBoard = state.board.map(row => [...row]);
  const from = fromAlgebraic(move.substring(0, 2));
  const to = fromAlgebraic(move.substring(2, 4));
  const promo = move.length > 4 ? move[4] : null;
  const piece = newBoard[from.row][from.col];
  newBoard[to.row][to.col] = promo ? (isWhite(piece) ? promo.toUpperCase() : promo) : piece;
  newBoard[from.row][from.col] = null;
  if (piece && piece.toLowerCase() === 'k') {
    if (move === 'e1g1') { newBoard[7][5] = newBoard[7][7]; newBoard[7][7] = null; }
    if (move === 'e1c1') { newBoard[7][3] = newBoard[7][0]; newBoard[7][0] = null; }
    if (move === 'e8g8') { newBoard[0][5] = newBoard[0][7]; newBoard[0][7] = null; }
    if (move === 'e8c8') { newBoard[0][3] = newBoard[0][0]; newBoard[0][0] = null; }
  }
  if (piece && piece.toLowerCase() === 'p' && move.substring(2, 4) === state.enPassant) {
    const epRow = state.turn === 'w' ? to.row + 1 : to.row - 1;
    newBoard[epRow][to.col] = null;
  }
  return { board: newBoard, turn: state.turn === 'w' ? 'b' : 'w', castling: state.castling, enPassant: '-' };
}

function minimax(state, depth, alpha, beta, max) {
  if (depth === 0) return { score: evaluate(state), move: null };
  const moves = generateMoves(state);
  if (moves.length === 0) return { score: max ? -100000 : 100000, move: null };
  let best = moves[0];
  if (max) {
    let maxS = -Infinity;
    for (const m of moves) {
      const r = minimax(makeMove(state, m), depth - 1, alpha, beta, false);
      if (r.score > maxS) { maxS = r.score; best = m; }
      alpha = Math.max(alpha, r.score);
      if (beta <= alpha) break;
    }
    return { score: maxS, move: best };
  } else {
    let minS = Infinity;
    for (const m of moves) {
      const r = minimax(makeMove(state, m), depth - 1, alpha, beta, true);
      if (r.score < minS) { minS = r.score; best = m; }
      beta = Math.min(beta, r.score);
      if (beta <= alpha) break;
    }
    return { score: minS, move: best };
  }
}

function findBestMove(fen) {
  const state = parseFEN(fen);
  const moves = generateMoves(state);
  if (moves.length === 0) return null;
  let depth = skillLevel < 5 ? 1 : skillLevel < 10 ? 2 : skillLevel < 15 ? 3 : 3;
  if (skillLevel < 5 && Math.random() < 0.4) return moves[Math.floor(Math.random() * moves.length)];
  const result = minimax(state, depth, -Infinity, Infinity, state.turn === 'b');
  if (skillLevel < 15 && Math.random() < 0.15) {
    const alt = moves.filter(m => Math.abs(evaluate(makeMove(state, m)) - result.score) < 150);
    if (alt.length > 0) return alt[Math.floor(Math.random() * alt.length)];
  }
  return result.move;
}

let currentFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
let currentMoves = [];

onmessage = function(e) {
  const cmd = e.data.trim();
  const parts = cmd.split(' ');
  switch (parts[0]) {
    case 'uci':
      postMessage('id name myChessBot');
      postMessage('id author mychess.fr');
      postMessage('option name Skill Level type spin default 10 min 0 max 20');
      postMessage('uciok');
      break;
    case 'isready':
      postMessage('readyok');
      break;
    case 'ucinewgame':
      currentFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
      currentMoves = [];
      break;
    case 'setoption':
      if (parts[2] === 'Skill' && parts[3] === 'Level') skillLevel = parseInt(parts[5]) || 10;
      break;
    case 'position':
      if (parts[1] === 'startpos') {
        currentFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
        currentMoves = parts[2] === 'moves' ? parts.slice(3) : [];
      } else if (parts[1] === 'fen') {
        const idx = parts.indexOf('moves');
        currentFen = idx === -1 ? parts.slice(2).join(' ') : parts.slice(2, idx).join(' ');
        currentMoves = idx === -1 ? [] : parts.slice(idx + 1);
      }
      break;
    case 'go':
      let state = parseFEN(currentFen);
      for (const m of currentMoves) state = makeMove(state, m);
      let fen = '';
      for (let r = 0; r < 8; r++) {
        let empty = 0;
        for (let c = 0; c < 8; c++) {
          if (state.board[r][c]) { if (empty > 0) { fen += empty; empty = 0; } fen += state.board[r][c]; }
          else empty++;
        }
        if (empty > 0) fen += empty;
        if (r < 7) fen += '/';
      }
      fen += ' ' + state.turn + ' ' + state.castling + ' ' + state.enPassant + ' 0 1';
      const best = findBestMove(fen);
      postMessage('info depth 3 score cp ' + evaluate(state));
      postMessage('bestmove ' + (best || '(none)'));
      break;
    case 'quit':
      close();
      break;
  }
};
