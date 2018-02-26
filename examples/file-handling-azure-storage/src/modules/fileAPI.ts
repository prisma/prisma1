import { v4 as uuid } from 'uuid';
import * as mime from 'mime-types';
import * as multiparty from 'multiparty';

const azure = require('azure');
const azureStorage = require('azure-storage');

export default ({ prisma }) => (req, res) => {
  let form = new multiparty.Form();
  let blobService = azure.createBlobService();
  var startDate = new Date();
  var expiryDate = new Date(startDate);
  expiryDate.setMinutes(startDate.getMinutes() + 100);
  startDate.setMinutes(startDate.getMinutes() - 100);

  var sharedAccessPolicy = {
    AccessPolicy: {
      Permissions: azureStorage.BlobUtilities.SharedAccessPermissions.READ,
      Start: startDate,
      Expiry: expiryDate
    }
  };
  // tslint:disable-next-line:no-any
  form.on('part', async function(part: any) {
    if (part.name !== 'data') {
      return;
    }

    const secret = uuid();
    const name = `${secret}_${part.filename}`;
    const size = part.byteCount;
    const contentType = mime.lookup(part.filename);
    const container = 'unizonn';
    
    blobService.createBlockBlobFromStream(
      // tslint:disable-next-line:typedef
      container, name, part, size , async function(error, result, response) {
        if (error) {
          return res.sendStatus(500);
        } else {
          const token = blobService.generateSharedAccessSignature(container, result.name, sharedAccessPolicy);
          const url = blobService.getUrl(container, result.name, token);
          
          try {
            const data = {
              name,
              size,
              secret,
              contentType,
              url,
            };
      
            const { id }: { id: string } = await prisma.mutation.createFile({ data }, ` { id } `);

            const file = {
              id,
              name,
              secret,
              contentType,
              size,
              url,
            };
            return res.status(200).send(result);
          } catch (err) {
            return res.sendStatus(500);
          }
    
        }
      }
    );
  });

  form.on('error', err => {
    return res.sendStatus(500);
  });

  form.parse(req);
};
