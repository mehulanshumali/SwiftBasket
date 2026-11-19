// Quantity stepper — used on product detail and cart lines

interface Props {
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  disabled?: boolean
}

export function QuantityStepper({ value, onChange, min = 1, max = 99, disabled }: Props) {
  return (
    <div className="flex h-10 items-center rounded-md border">
      <button
        type="button"
        aria-label="Decrease quantity"
        disabled={disabled || value <= min}
        onClick={() => onChange(value - 1)}
        className="h-full w-10 text-lg transition-colors hover:text-primary disabled:opacity-30"
      >
        −
      </button>
      <span className="w-10 text-center text-sm font-medium tabular-nums">{value}</span>
      <button
        type="button"
        aria-label="Increase quantity"
        disabled={disabled || value >= max}
        onClick={() => onChange(value + 1)}
        className="h-full w-10 text-lg transition-colors hover:text-primary disabled:opacity-30"
      >
        +
      </button>
    </div>
  )
}
