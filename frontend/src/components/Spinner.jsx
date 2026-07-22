import { Loader2 } from 'lucide-react';

function Spinner({ label = 'Loading…' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 min-h-[50vh]">
      <Loader2 className="w-10 h-10 text-emerald-600 animate-spin" />
      <p className="text-gray-600 dark:text-gray-400">{label}</p>
    </div>
  );
}

export default Spinner;
