import java.util.ArrayList;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory
   as it would overflow memory
 */

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private BlockNode biggestNode;
    private TransactionPool transactPool;
    private HashMap<ByteArrayWrapper, BlockNode> chain;

    // all information required in handling a block in block chain
    private class BlockNode {
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        // utxo pool for making a new block on top of this block
        private UTXOPool uPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<BlockNode>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
        }
    }

    /*
     * create an empty block chain with just a genesis block.
     * Assume genesis block is a valid block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        chain = new HashMap<>();
        UTXOPool uniqueUtxo = new UTXOPool();
        addCoinbaseCoinToUTXOPool(genesisBlock, uniqueUtxo);
        BlockNode genesisNode = new BlockNode(genesisBlock, null, uniqueUtxo);
        chain.put(byteWrapper(genesisBlock.getHash()), genesisNode);
        transactPool = new TransactionPool();
        biggestNode = genesisNode;
    }

    /*
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return biggestNode.b;
    }

    /*
     * Get the UTXOPool for mining a new block on top of
     * max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return biggestNode.getUTXOPoolCopy();
    }

    /*
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return transactPool;
    }

    /*
     * Add a block to block chain if it is valid.
     * For validity, all transactions should be valid
     * and block should be at height > (maxHeight - CUT_OFF_AGE).
     * For example, you can try creating a new block over genesis block
     * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1.
     * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height
     * 2.
     * Return true of block is successfully added
     */
    public boolean addBlock(Block b) {
        // IMPLEMENT THIS
        byte[] prevHashBlock = b.getPrevBlockHash();
        if (prevHashBlock == null) {
            return false;
        }

        BlockNode parentNode = chain.get(byteWrapper(prevHashBlock));
        if (parentNode == null) {
            return false;
        }

        TxHandler txHandler = new TxHandler(parentNode.getUTXOPoolCopy());
        Transaction[] transacts = b.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTransacts = txHandler.handleTxs(transacts);

        if (validTransacts.length != transacts.length) {
            return false;
        }

        int newBlockHeight = parentNode.height + 1;
        if (newBlockHeight <= biggestNode.height - CUT_OFF_AGE) {
            return false;
        }

        // Possibly Test #27 Solution: needs to be fixed or edited
        // String str1 = "parentNode.height (test27): ";
        // String str2 = "biggestNode.height: ";
        // String str3 = "biggestNode.height - CUT_OFF_AGE: ";
        // System.out.println(str1+parentNode.height);
        // System.out.println(str2+biggestNode.height);
        // System.out.println(biggestNode.height - CUT_OFF_AGE);
        if (parentNode.height < biggestNode.height - CUT_OFF_AGE) {
            return false;
        }

        UTXOPool uniqueUtxo = txHandler.getUTXOPool();
        addCoinbaseCoinToUTXOPool(b, uniqueUtxo);
        BlockNode tempNode = new BlockNode(b, parentNode, uniqueUtxo);
        chain.put(byteWrapper(b.getHash()), tempNode);

        if (newBlockHeight > biggestNode.height) {
            biggestNode = tempNode;
        }
        return true;
    }

    /*
     * Add a transaction in transaction pool
     */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        transactPool.addTransaction(tx);
    }

    // Wrapper function to wrap the passed in byte array of hash values
    // Needs to be done to better input into the blockchain
    private static ByteArrayWrapper byteWrapper(byte[] hashByteArr) {
        ByteArrayWrapper tempByteWrapper = new ByteArrayWrapper(hashByteArr);
        return tempByteWrapper;
    }

    // Function for adding a block as a coinbase coin to the current UTXOPool
    private void addCoinbaseCoinToUTXOPool(Block b, UTXOPool coinPool) {
        // generates a new coinbase transaction for the block
        Transaction coin = b.getCoinbase();

        for (int i = 0; i < coin.numOutputs(); i++) {
            Transaction.Output out = coin.getOutput(i);
            UTXO utxo = new UTXO(coin.getHash(), i);
            // add the coinbase UTXO to the current UTXOPool
            coinPool.addUTXO(utxo, out);
        }
    }
}
