import { BrowserRouter, Routes, Route } from 'react-router-dom';
import GameController from './screens/GameController';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<div className="flex h-screen items-center justify-center text-2xl text-gray-400">Please scan a QR code from the host to join a game.</div>} />
        <Route path="/game/:gameId" element={<GameController />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
