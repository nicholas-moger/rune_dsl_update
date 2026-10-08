package holdout.onlyexistsitemroot;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import holdout.onlyexistsitemroot.meta.PathsMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * only-exists targets: a bare, a nested, a choice-typed and a multi attribute - every one optional.
 * @version 0.0.0
 */
@RosettaDataType(value="Paths", builder=Paths.PathsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Paths", model="holdout", builder=Paths.PathsBuilderImpl.class, version="0.0.0")
public interface Paths extends RosettaModelObject {

	PathsMeta metaData = new PathsMeta();

	/*********************** Getter Methods  ***********************/
	String getP();
	String getQ();
	Sub getSub();
	Pick getPick();

	/*********************** Build Methods  ***********************/
	Paths build();
	
	Paths.PathsBuilder toBuilder();
	
	static Paths.PathsBuilder builder() {
		return new Paths.PathsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Paths> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Paths> getType() {
		return Paths.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
		processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
		processRosetta(path.newSubPath("sub"), processor, Sub.class, getSub());
		processRosetta(path.newSubPath("pick"), processor, Pick.class, getPick());
	}
	

	/*********************** Builder Interface  ***********************/
	interface PathsBuilder extends Paths, RosettaModelObjectBuilder {
		Sub.SubBuilder getOrCreateSub();
		@Override
		Sub.SubBuilder getSub();
		Pick.PickBuilder getOrCreatePick();
		@Override
		Pick.PickBuilder getPick();
		Paths.PathsBuilder setP(String p);
		Paths.PathsBuilder setQ(String q);
		Paths.PathsBuilder setSub(Sub sub);
		Paths.PathsBuilder setPick(Pick pick);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
			processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
			processRosetta(path.newSubPath("sub"), processor, Sub.SubBuilder.class, getSub());
			processRosetta(path.newSubPath("pick"), processor, Pick.PickBuilder.class, getPick());
		}
		

		Paths.PathsBuilder prune();
	}

	/*********************** Immutable Implementation of Paths  ***********************/
	class PathsImpl implements Paths {
		private final String p;
		private final String q;
		private final Sub sub;
		private final Pick pick;
		
		protected PathsImpl(Paths.PathsBuilder builder) {
			this.p = builder.getP();
			this.q = builder.getQ();
			this.sub = ofNullable(builder.getSub()).map(f->f.build()).orElse(null);
			this.pick = ofNullable(builder.getPick()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@Override
		@RosettaAttribute("sub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("sub")
		public Sub getSub() {
			return sub;
		}
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public Pick getPick() {
			return pick;
		}
		
		@Override
		public Paths build() {
			return this;
		}
		
		@Override
		public Paths.PathsBuilder toBuilder() {
			Paths.PathsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Paths.PathsBuilder builder) {
			ofNullable(getP()).ifPresent(builder::setP);
			ofNullable(getQ()).ifPresent(builder::setQ);
			ofNullable(getSub()).ifPresent(builder::setSub);
			ofNullable(getPick()).ifPresent(builder::setPick);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Paths _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			if (!Objects.equals(sub, _that.getSub())) return false;
			if (!Objects.equals(pick, _that.getPick())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			_result = 31 * _result + (sub != null ? sub.hashCode() : 0);
			_result = 31 * _result + (pick != null ? pick.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Paths {" +
				"p=" + this.p + ", " +
				"q=" + this.q + ", " +
				"sub=" + this.sub + ", " +
				"pick=" + this.pick +
			'}';
		}
	}

	/*********************** Builder Implementation of Paths  ***********************/
	class PathsBuilderImpl implements Paths.PathsBuilder {
	
		protected String p;
		protected String q;
		protected Sub.SubBuilder sub;
		protected Pick.PickBuilder pick;
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@Override
		@RosettaAttribute("sub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("sub")
		public Sub.SubBuilder getSub() {
			return sub;
		}
		
		@Override
		public Sub.SubBuilder getOrCreateSub() {
			Sub.SubBuilder result;
			if (sub!=null) {
				result = sub;
			}
			else {
				result = sub = Sub.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public Pick.PickBuilder getPick() {
			return pick;
		}
		
		@Override
		public Pick.PickBuilder getOrCreatePick() {
			Pick.PickBuilder result;
			if (pick!=null) {
				result = pick;
			}
			else {
				result = pick = Pick.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("p")
		@Override
		public Paths.PathsBuilder setP(String _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@RosettaAttribute("q")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("q")
		@Override
		public Paths.PathsBuilder setQ(String _q) {
			this.q = _q == null ? null : _q;
			return this;
		}
		
		@RosettaAttribute("sub")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("sub")
		@Override
		public Paths.PathsBuilder setSub(Sub _sub) {
			this.sub = _sub == null ? null : _sub.toBuilder();
			return this;
		}
		
		@RosettaAttribute("pick")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pick")
		@Override
		public Paths.PathsBuilder setPick(Pick _pick) {
			this.pick = _pick == null ? null : _pick.toBuilder();
			return this;
		}
		
		@Override
		public Paths build() {
			return new Paths.PathsImpl(this);
		}
		
		@Override
		public Paths.PathsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Paths.PathsBuilder prune() {
			if (sub!=null && !sub.prune().hasData()) sub = null;
			if (pick!=null && !pick.prune().hasData()) pick = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getP()!=null) return true;
			if (getQ()!=null) return true;
			if (getSub()!=null && getSub().hasData()) return true;
			if (getPick()!=null && getPick().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Paths.PathsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Paths.PathsBuilder o = (Paths.PathsBuilder) other;
			
			merger.mergeRosetta(getSub(), o.getSub(), this::setSub);
			merger.mergeRosetta(getPick(), o.getPick(), this::setPick);
			
			merger.mergeBasic(getP(), o.getP(), this::setP);
			merger.mergeBasic(getQ(), o.getQ(), this::setQ);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Paths _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			if (!Objects.equals(sub, _that.getSub())) return false;
			if (!Objects.equals(pick, _that.getPick())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			_result = 31 * _result + (sub != null ? sub.hashCode() : 0);
			_result = 31 * _result + (pick != null ? pick.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PathsBuilder {" +
				"p=" + this.p + ", " +
				"q=" + this.q + ", " +
				"sub=" + this.sub + ", " +
				"pick=" + this.pick +
			'}';
		}
	}
}
