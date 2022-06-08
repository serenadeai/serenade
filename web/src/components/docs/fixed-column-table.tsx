import React from "react";

export const FixedColumnTable: React.FC<{ columns: number; items: string[] }> = ({
  columns,
  items,
}) => {
  let rows: string[][] = Array.from({ length: Math.ceil(items.length / columns) }, () => []);
  for (let i = 0; i < rows.length; i++) {
    for (let j = 0; j < columns; j++) {
      rows[i].push(items[i * columns + j]);
    }
  }

  return (
    <table className="my-4">
      <tbody>
        {rows.map((row, i) => (
          <tr key={i} className={i % 2 == 1 ? "bg-gray-100" : ""}>
            {row.map((e, j) => (
              <td key={i + " " + j} className="p-2">
                {e}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
};
