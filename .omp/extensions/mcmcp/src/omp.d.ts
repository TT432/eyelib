/**
 * Minimal ambient declarations for the Oh My Pi (omp) extension API surface
 * used by the mcmcp extension. Sourced from omp://extensions.md.
 *
 * At runtime omp injects the host-bundled API into the factory; these exist
 * only for standalone type-checking / editor support.
 */

export interface TextBlock {
  type: "text";
  text: string;
}

export interface AgentToolResult {
  content: TextBlock[];
  details?: unknown;
  isError?: boolean;
}

export interface ExtensionUIContext {
  notify(message: string, level?: "info" | "warning" | "error"): void;
}

export interface ExtensionContext {
  ui: ExtensionUIContext;
  hasUI: boolean;
  /** Current session working directory (project root). */
  cwd: string;
  isIdle(): boolean;
}

/**
 * Opaque zod schema handle. pi.zod is the injected zod/v4 module; schemas are
 * erased at runtime, so only the fluent surface used by this extension is
 * declared here.
 */
export interface ZodSchema {
  optional(): ZodSchema;
  default(value: unknown): ZodSchema;
  describe(text: string): ZodSchema;
  min(n: number): ZodSchema;
  max(n: number): ZodSchema;
  int(): ZodSchema;
}

export interface ZodModule {
  object(shape: Record<string, ZodSchema>): ZodSchema;
  string(): ZodSchema;
  number(): ZodSchema;
  boolean(): ZodSchema;
  enum(values: readonly string[]): ZodSchema;
}

export interface ToolDefinition<TParams> {
  name: string;
  label: string;
  description: string;
  /** zod schema built from pi.zod. */
  parameters: ZodSchema;
  hidden?: boolean;
  execute(
    toolCallId: string,
    params: TParams,
    signal: AbortSignal | undefined,
    onUpdate: ((partial: AgentToolResult) => void) | undefined,
    ctx: ExtensionContext,
  ): Promise<AgentToolResult>;
}

export interface ExtensionLogger {
  info(message: string, data?: unknown): void;
  warn(message: string, data?: unknown): void;
  error(message: string, data?: unknown): void;
}

export interface ExtensionAPI {
  /** Injected zod/v4 module (minimal declared surface). */
  readonly zod: ZodModule;
  readonly logger: ExtensionLogger;
  setLabel(label: string): void;
  /** Method-style parameter is bivariant, so concrete ToolDefinition<T> registers fine. */
  registerTool(definition: ToolDefinition<never>): void;
  on(event: string, handler: (event: unknown, ctx: ExtensionContext) => unknown): void;
}
