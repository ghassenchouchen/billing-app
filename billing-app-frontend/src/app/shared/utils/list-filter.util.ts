
export interface FilterOptions<T> {
  search?: string;
  searchFields: (keyof T)[];
  statusField?: keyof T;
  statusValue?: string;
}

export function filterList<T>(data: T[], opts: FilterOptions<T>): T[] {
  let result = data;

  if (opts.search) {
    const term = opts.search.toLowerCase();
    result = result.filter(item =>
      opts.searchFields.some(field => {
        const val = item[field];
        return val != null && String(val).toLowerCase().includes(term);
      })
    );
  }

  if (opts.statusValue && opts.statusField) {
    const field = opts.statusField;
    result = result.filter(item => String(item[field]) === opts.statusValue);
  }

  return result;
}
