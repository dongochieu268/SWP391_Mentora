# BookNest State Diagram Replacement Design

## Goal

Validate the order-lifecycle state model in `BookNest_BlackBox_Test_Lab_filled.docx`, redraw it as an editable draw.io diagram, export a clear PNG, and replace the existing embedded image directly in the same DOCX.

## Verified State Model

- Start state: `New`
- End states: `Delivered` and `Cancelled`
- `New --pay--> Paid`
- `New --cancel--> Cancelled`
- `Paid --ship--> Shipped`
- `Paid --cancel (refund)--> Cancelled`
- `Shipped --deliver--> Delivered`
- Invalid transitions are excluded because Task 2.2 asks for valid lifecycle transitions only.

## Diagram Design

- Use a left-to-right primary path: `New`, `Paid`, `Shipped`, `Delivered`.
- Place `Cancelled` below the primary path so both cancellation transitions remain readable.
- Show an incoming start arrow pointing to `New`.
- Render `Delivered` and `Cancelled` with double circles to mark terminal states.
- Use black text and connectors on a white background for print clarity.
- Label the Paid cancellation edge `cancel (refund)` so the event and its effect are distinct.

## Deliverables

- An editable native draw.io file in the workspace.
- A high-resolution PNG exported from draw.io.
- The original DOCX updated in place by replacing `word/media/image1.png` while preserving all other package entries and document content.
- A backup copy of the original DOCX created beside the source before replacement.

## Verification

- Validate the draw.io XML as well-formed XML.
- Confirm all five valid transitions and both terminal states are visible in the exported PNG.
- Confirm the updated DOCX opens as a valid ZIP package and still contains all original entries.
- Re-read the embedded image from the updated DOCX and compare it with the exported PNG.
