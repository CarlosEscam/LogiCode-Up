// id: necesario cuando el mismo campo aparece en varios formularios de una página.
export function Field({ label, name, error, hint, id: customId, ...inputProps }) {
  const id = customId ?? `campo-${name}`
  const describedBy = [hint && `${id}-ayuda`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} name={name} aria-invalid={error ? true : undefined} aria-describedby={describedBy} {...inputProps} />
      {hint && (
        <small id={`${id}-ayuda`} className="field__hint">
          {hint}
        </small>
      )}
      {error && (
        <small id={`${id}-error`} className="field__error">
          {error}
        </small>
      )}
    </div>
  )
}

export function FormStatus({ error, success }) {
  if (error) {
    return (
      <p className="notice notice--error" role="alert">
        {error}
      </p>
    )
  }
  if (success) {
    return (
      <p className="notice notice--ok" role="status">
        {success}
      </p>
    )
  }
  return null
}
