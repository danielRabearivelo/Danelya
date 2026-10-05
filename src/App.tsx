import { useState } from 'react';
import { AndroidSimulator } from './components/AndroidSimulator';
import { CodeExplorer } from './components/CodeExplorer';
import { UnitTestsRunner } from './components/UnitTestsRunner';
import { Smartphone, Code, CheckCircle, ShieldCheck, Download } from 'lucide-react';

export default function App() {
  const [activeView, setActiveView] = useState<'simulator' | 'code' | 'tests'>('simulator');

  return (
    <div className="min-h-screen bg-slate-100/70 text-slate-900 font-sans antialiased flex flex-col">
      {/* Top Bar following Top Bar Contract */}
      <header className="bg-white border-b border-slate-200/80 px-4 sm:px-8 py-3.5 sticky top-0 z-40 shadow-xs">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
          {/* Zone 1: Wordmark */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-slate-900 text-white flex items-center justify-center font-bold text-sm shadow-xs">
              CA
            </div>
            <div>
              <span className="text-base font-bold text-slate-900 tracking-tight block">
                Caisse Association
              </span>
              <span className="text-[11px] text-slate-500 hidden sm:block">
                Android 14 · Kotlin · Room · Jetpack Compose · 100% Hors-ligne
              </span>
            </div>
          </div>

          {/* Zone 2: Navigation Tabs */}
          <nav className="flex items-center bg-slate-100 p-1 rounded-xl">
            <button
              onClick={() => setActiveView('simulator')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                activeView === 'simulator'
                  ? 'bg-white text-slate-900 shadow-xs font-semibold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span className="whitespace-nowrap">Simulateur Mobile</span>
            </button>

            <button
              onClick={() => setActiveView('code')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                activeView === 'code'
                  ? 'bg-white text-slate-900 shadow-xs font-semibold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Code className="w-3.5 h-3.5" />
              <span className="whitespace-nowrap">Code Source Android</span>
            </button>

            <button
              onClick={() => setActiveView('tests')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                activeView === 'tests'
                  ? 'bg-white text-slate-900 shadow-xs font-semibold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
              <span className="whitespace-nowrap">Tests Unitaires</span>
            </button>
          </nav>

          {/* Zone 3: Actions */}
          <div className="hidden lg:flex items-center gap-3">
            <span className="text-xs text-slate-500 flex items-center gap-1">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              Zéro permission Internet
            </span>
          </div>
        </div>
      </header>

      {/* Main View Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6">
        {activeView === 'simulator' && (
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 bg-white p-4 rounded-2xl border border-slate-200">
              <div>
                <h2 className="text-sm font-semibold text-slate-900">
                  Simulateur Interactif - Application Trésorier
                </h2>
                <p className="text-xs text-slate-500">
                  Testez en conditions réelles toutes les règles : répartition FIFO des cotisations, versements à la caisse, génération de l'état PDF avec signatures, relances WhatsApp/SMS, et sauvegarde SAF.
                </p>
              </div>
              <button
                onClick={() => setActiveView('code')}
                className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-700 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 px-3 py-1.5 rounded-lg transition-colors cursor-pointer whitespace-nowrap self-start sm:self-auto"
              >
                <Code className="w-3.5 h-3.5" />
                Voir le code Kotlin Android
              </button>
            </div>

            <AndroidSimulator />
          </div>
        )}

        {activeView === 'code' && (
          <div className="space-y-4">
            <CodeExplorer />
          </div>
        )}

        {activeView === 'tests' && (
          <div className="space-y-4">
            <UnitTestsRunner />
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-3 px-6 text-center text-xs text-slate-500">
        <p>Caisse Association · Architecture MVVM + Room + StateFlow + Jetpack Compose · Android 14 (API 34)</p>
      </footer>
    </div>
  );
}
