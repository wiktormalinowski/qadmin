/**
 * Reprezentuje zagregowany widok encji zawierający nazwę, metadane i właściwe dane.
 */
export interface EntityViewModel {
  name: string;
  metadata: unknown;
  data: unknown;
}
